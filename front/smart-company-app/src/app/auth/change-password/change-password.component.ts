import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { User, UserType } from '../model/user';
import { Router } from '@angular/router';
import { AxiosService } from '../axios.service';
import { passwordMatchValidator } from '../../common/validators/password-match.validator';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [FormsModule, CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.css'
})
export class ChangePasswordComponent {
  submitting = false;
  errorMessage = '';

  changePasswordForm: FormGroup;

  constructor(private axios: AxiosService, private formBuilder: FormBuilder, private router: Router) { 

    this.changePasswordForm = this.formBuilder.group(
      {
        password: ["", [Validators.required, Validators.minLength(5)]],
        confirmPassword: ["", [Validators.required, Validators.minLength(5)]],
      },
      {
        validators: passwordMatchValidator('password', 'confirmPassword')
      }
    );

  }

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    const submitData = this.changePasswordForm.value;

    this.axios.request('PUT', '/account/change-password', { newPassword: submitData.password })
      .then(response => {
        this.submitting = false;
        this.axios.setAuthToken(response.data.token);
        this.router.navigate(['products']);
      })
      .catch(error => {
        this.submitting = false;
        this.errorMessage = error?.response?.data?.message || 'Failed to change password. Please try again.';
      });
  }

}