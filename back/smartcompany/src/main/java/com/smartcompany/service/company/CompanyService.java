package com.smartcompany.service.company;

import com.smartcompany.dto.company.CompanyDecisionDto;
import com.smartcompany.dto.company.CompanyDetailDto;
import com.smartcompany.dto.company.CompanyRequestDto;
import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.company.Company;
import com.smartcompany.model.company.CompanyDocument;
import com.smartcompany.model.company.CompanyImage;
import com.smartcompany.model.company.CompanyStatus;
import com.smartcompany.model.location.City;
import com.smartcompany.model.location.Country;
import com.smartcompany.model.user.User;
import com.smartcompany.repository.company.CompanyRepository;
import com.smartcompany.repository.location.CityRepository;
import com.smartcompany.repository.location.CountryRepository;
import com.smartcompany.repository.user.UserRepository;
import com.smartcompany.service.user.EmailService;
import com.smartcompany.specification.CompanySpecification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private final CompanyRepository companyRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CompanyService self; // self-injection, za pravilan @Transactional/@Async proxy poziv

    @Autowired
    public CompanyService(
            CompanyRepository companyRepository,
            CountryRepository countryRepository,
            CityRepository cityRepository,
            UserRepository userRepository,
            EmailService emailService,
            @Lazy CompanyService self) {
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.self = self;
    }

    @Transactional
    public CompanyDetailDto createRequest(String ownerUsername, CompanyRequestDto dto) {
        User owner = userRepository.findById(ownerUsername)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Country country = countryRepository.findById(dto.countryId())
            .orElseThrow(() -> new AppException("Country not found", HttpStatus.NOT_FOUND));
        City city = cityRepository.findById(dto.cityId())
            .orElseThrow(() -> new AppException("City not found", HttpStatus.NOT_FOUND));

        Company company = new Company();
        company.setName(dto.name());
        company.setAddress(dto.address());
        company.setCountry(country);
        company.setCity(city);
        company.setLatitude(dto.latitude());
        company.setLongitude(dto.longitude());
        company.setOwner(owner);
        company.setStatus(CompanyStatus.PENDING);

        if (dto.imageKeys() != null) {
            for (String key : dto.imageKeys()) {
                CompanyImage img = new CompanyImage();
                img.setImageKey(key);
                img.setCompany(company);
                company.getImages().add(img);
            }
        }

        if (dto.documents() != null) {
            for (CompanyRequestDto.DocumentDto docDto : dto.documents()) {
                CompanyDocument doc = new CompanyDocument();
                doc.setFileKey(docDto.fileKey());
                doc.setFileType(docDto.fileType());
                doc.setCompany(company);
                company.getDocuments().add(doc);
            }
        }

        return toDto(companyRepository.save(company));
    }

    private CompanyDetailDto toDto(Company c) {
        List<String> imageKeys = c.getImages().stream()
            .map(CompanyImage::getImageKey).toList();
        List<CompanyDetailDto.DocumentDto> documents = c.getDocuments().stream()
            .map(d -> new CompanyDetailDto.DocumentDto(d.getFileKey(), d.getFileType()))
            .toList();

        return new CompanyDetailDto(
            c.getId(),
            c.getName(),
            c.getAddress(),
            new CountryDto(c.getCountry().getId(), c.getCountry().getName(), c.getCountry().getCode()),
            new CityDto(c.getCity().getId(), c.getCity().getName(), c.getCity().getCountry().getId()),
            c.getLatitude(),
            c.getLongitude(),
            c.getStatus().toString(),
            c.getRejectionReason(),
            c.getOwner().getUsername(),
            imageKeys,
            documents
        );
    }
    // ─── Za menadžera ────────────────────────────────────────────────────────

    public Page<CompanyDetailDto> search(String search, CompanyStatus status, Pageable pageable) {
        Specification<Company> spec = CompanySpecification.search(search, status);
        return companyRepository.findAll(spec, pageable).map(this::toDto);
    }

    public CompanyDetailDto getByIdForManager(Long id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new AppException("Company not found", HttpStatus.NOT_FOUND));
        return toDto(company);
    }

    /**
     * Javni ulaz — NIJE @Transactional. Deleguje kritični, zaključavajući deo
     * u self.decideTransactional() (kratka transakcija), a slanje email-a se
     * radi OVDE, van transakcije, da se pesimistički lock nad Company redom
     * ne drži tokom sporog SMTP poziva.
     */
    public CompanyDetailDto decide(Long id, CompanyDecisionDto dto) {
        Company saved = self.decideTransactional(id, dto);

        self.sendDecisionEmailAsync(saved.getId());

        return toDto(saved);
    }

    @Transactional
    public Company decideTransactional(Long id, CompanyDecisionDto dto) {
        Company company;
        try {
            // PESIMISTIČKO ZAKLJUČAVANJE: findByIdForUpdate radi SELECT ... FOR
            // UPDATE — drugi menadžer koji pokuša da obradi isti zahtev čeka dok
            // se ova transakcija ne završi, umesto da oba istovremeno prođu
            // proveru statusa.
            company = companyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException("Company not found", HttpStatus.NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            throw new AppException(
                "Zahtev trenutno obrađuje drugi menadžer, pokušajte ponovo",
                HttpStatus.CONFLICT
            );
        }

        if (company.getStatus() != CompanyStatus.PENDING) {
            throw new AppException("Company request already processed", HttpStatus.BAD_REQUEST);
        }

        if (dto.approve()) {
            company.setStatus(CompanyStatus.APPROVED);
            company.setRejectionReason(null);
        } else {
            if (dto.rejectionReason() == null || dto.rejectionReason().isBlank()) {
                throw new AppException("Rejection reason is required", HttpStatus.BAD_REQUEST);
            }
            company.setStatus(CompanyStatus.REJECTED);
            company.setRejectionReason(dto.rejectionReason());
        }

        return companyRepository.save(company);
    }

    /**
     * Radi u posebnom threadu (invoiceExecutor pool, već definisan u AsyncConfig
     * za OrderService) — ne blokira response korisniku niti drži DB konekciju
     * dok SMTP poziv traje.
     */
    @Async("invoiceExecutor")
    @Transactional(readOnly = true)
    public void sendDecisionEmailAsync(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new AppException("Company not found", HttpStatus.NOT_FOUND));

        if (!emailEnabled) {
            // System.out.println("[CompanyService] Email disabled (load testing mode) - skipping decision email for company " + companyId);
            return;
        }

        try {
            emailService.sendCompanyDecisionEmail(
                company.getOwner().getEmail(),
                company.getName(),
                company.getStatus(),
                company.getRejectionReason()
            );
        } 
        catch (Exception e) {
            System.err.println("[CompanyService] Failed to send decision email for company " + companyId + ": " + e.getMessage());
        }
    }
}