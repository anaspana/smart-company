import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormGroup, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { AxiosService } from '../axios.service';
import { UserType, User } from '../model/user';
import { FileService } from '../../file.service';
import { HttpClient } from '@angular/common/http';
import { passwordMatchValidator } from '../../common/validators/password-match.validator';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {

  selectedFile: File | null = null;
  preSignedUrl: string = '';
  
  fileError: boolean = false;
  errorMessage: string = '';

  submitting: boolean = false;
  successMessage: string = '';

  formSubmitted: boolean = false;

  registerForm: FormGroup;

  constructor(public axios: AxiosService, private formBuilder: FormBuilder, private router: Router, private fileService: FileService, private http: HttpClient) {
    this.registerForm = this.formBuilder.group(
      {
        email: ["", Validators.compose([Validators.required, Validators.email])],
        password: ["", Validators.compose([Validators.required, Validators.minLength(5)])],
        confirmPassword: ["", Validators.compose([Validators.required, Validators.minLength(5)])],
        username: ["", Validators.compose([Validators.required, Validators.minLength(5)])],
        file: [null, Validators.compose([Validators.required])],
        // type: ["Guest", [Validators.required]]
      },
      {
        validators: passwordMatchValidator('password', 'confirmPassword')
      }
    );
  }

  onFileSelected(event: any, label: HTMLLabelElement) {

    const file = event.target.files?.[0];

    // Ako nema fajla
    if (!file) {
      label.classList.remove('selected');
      label.innerText = 'Choose file';
      this.selectedFile = null;
      return;
    }

    const validTypes = [
      'image/jpeg',
      'image/png',
      'image/gif'
    ];

    // Ako tip nije validan
    if (!validTypes.includes(file.type)) {

      label.classList.remove('selected');
      label.innerText = 'Choose file';

      this.fileError = true;
      this.errorMessage = 'Invalid file type! Please select (.jpg, .png, .gif)';

      this.selectedFile = null;
      this.registerForm.get('file')?.setValue(null);

      return;
    }

    // Ako je sve OK
    label.classList.add('selected');
    label.innerText = file.name;

    this.fileError = false;
    this.errorMessage = '';

    this.selectedFile = file;
  }

  onSubmit(): void {
    this.formSubmitted = true;
    this.registerForm.markAllAsTouched();

    if (this.registerForm.invalid || !this.selectedFile) {
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    this.fileError = false;
    const data = this.registerForm.value;

    // 1. Prvo proveri username
    this.axios.request('GET', `/users/check-username?username=${data.username}`, null)
      .then(() => {
        // 2. Username slobodan — uploaduj sliku
        this.fileService.getUploadUrl(this.selectedFile!.name, 'profile-images')
          .subscribe({
            next: (response) => {
              const { url: uploadUrl, filename: s3Key } = response;
              this.fileService.uploadToS3(uploadUrl, this.selectedFile!)
                .subscribe({
                  next: () => {
                    const user = new User(
                      data.username, data.password, UserType.Customer,
                      false, false, data.email, s3Key
                    );
                    this.axios.request('POST', '/register/customer', user)
                      .then(() => {
                        this.submitting = false;
                        this.successMessage = 'Registration successful! Please check your email to activate your account.';
                        this.registerForm.reset();
                        this.registerForm.markAsUntouched();
                        this.registerForm.markAsPristine();
                        this.formSubmitted = false;
                        this.selectedFile = null;
                        setTimeout(() => this.router.navigate(['login']), 3000);
                      })
                      .catch(err => {
                        this.submitting = false;
                        this.errorMessage = err?.response?.data?.message || 'Registration failed. Please try again.';
                      });
                  },
                  error: () => {
                    this.submitting = false;
                    this.errorMessage = 'Image upload failed. Please try again.';
                  }
                });
            },
            error: () => {
              this.submitting = false;
              this.errorMessage = 'Could not get upload URL. Please try again.';
            }
          });
      })
      .catch(() => {
        // Username zauzet
        this.submitting = false;
        this.errorMessage = 'Username is already taken. Please choose another.';
      });
  }
}