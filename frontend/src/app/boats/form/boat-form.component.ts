import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BoatService } from '../../shared/services/boat.service';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';

@Component({
  selector: 'app-boat-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="page-container">
      <div class="breadcrumb">
        <a routerLink="/boats">Fleet</a>
        <mat-icon>chevron_right</mat-icon>
        <span *ngIf="isEditMode">Edit Boat</span>
        <span *ngIf="!isEditMode">Add Boat</span>
      </div>

      <app-loading-spinner *ngIf="loadingData" message="Loading..." />

      <mat-card class="form-card" *ngIf="!loadingData">
        <div class="form-header">
          <div class="form-icon">
            <mat-icon>{{ isEditMode ? 'edit' : 'add_circle' }}</mat-icon>
          </div>
          <div>
            <h1>{{ isEditMode ? 'Edit Boat' : 'Add New Boat' }}</h1>
            <p>{{ isEditMode ? 'Update the boat information below.' : 'Fill in the details to add a new boat to your fleet.' }}</p>
          </div>
        </div>

        <form [formGroup]="boatForm" (ngSubmit)="onSubmit()" class="boat-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Boat Name *</mat-label>
            <mat-icon matPrefix>directions_boat</mat-icon>
            <input matInput formControlName="name" placeholder="e.g. Sea Explorer" maxlength="40" />
            <mat-hint align="end">{{ boatForm.get('name')?.value?.length || 0 }}/40</mat-hint>
            <mat-error *ngIf="boatForm.get('name')?.hasError('required')">Name is required</mat-error>
            <mat-error *ngIf="boatForm.get('name')?.hasError('minlength')">Name must be at least 1 character</mat-error>
            <mat-error *ngIf="boatForm.get('name')?.hasError('maxlength')">Name must not exceed 40 characters</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <mat-icon matPrefix>description</mat-icon>
            <textarea
              matInput
              formControlName="description"
              placeholder="Describe this boat..."
              rows="4"
              maxlength="2000">
            </textarea>
            <mat-hint align="end">{{ boatForm.get('description')?.value?.length || 0 }}/2000</mat-hint>
            <mat-error *ngIf="boatForm.get('description')?.hasError('maxlength')">Description must not exceed 2000 characters</mat-error>
          </mat-form-field>

          <div class="form-actions">
            <a mat-stroked-button [routerLink]="isEditMode ? ['/boats', boatId] : ['/boats']">
              <mat-icon>close</mat-icon>
              Cancel
            </a>
            <button
              mat-raised-button
              color="primary"
              type="submit"
              [disabled]="boatForm.invalid || saving">
              <mat-icon>{{ saving ? 'hourglass_empty' : (isEditMode ? 'save' : 'add') }}</mat-icon>
              {{ saving ? 'Saving...' : (isEditMode ? 'Update Boat' : 'Add Boat') }}
            </button>
          </div>
        </form>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container {
      max-width: 900px;
      margin: 0 auto;
      padding: 28px 24px;
    }
    .breadcrumb {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-bottom: 24px;
      color: var(--color-text-secondary);
      font-size: 0.9rem;
      a { color: var(--color-primary); text-decoration: none; }
      a:hover { text-decoration: underline; }
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .form-card { padding: 32px; max-width: 640px; }
    .form-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 32px;
    }
    .form-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      mat-icon { color: white; font-size: 28px; width: 28px; height: 28px; }
    }
    h1 {
      font-family: var(--font-mono);
      font-size: 1.4rem;
      color: var(--color-primary);
    }
    p { color: var(--color-text-secondary); font-size: 0.9rem; margin-top: 4px; }
    .boat-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .full-width { width: 100%; }
    .form-actions {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
      margin-top: 8px;
    }
  `]
})
export class BoatFormComponent implements OnInit {
  boatForm!: FormGroup;
  isEditMode = false;
  boatId: number | null = null;
  loadingData = false;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private boatService: BoatService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.boatForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(40)]],
      description: ['', [Validators.maxLength(2000)]]
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.boatId = Number(id);
      this.loadBoat(this.boatId);
    }

    window.addEventListener('auth:token_received', this.tokenListener);
  }

  private tokenListener = () => {
    if (this.isEditMode && this.boatId) {
      this.loadBoat(this.boatId);
    }
  }

  ngOnDestroy(): void {
    window.removeEventListener('auth:token_received', this.tokenListener);
  }

  private loadBoat(id: number): void {
    this.loadingData = true;
    this.cdr.detectChanges();
    this.boatService.getBoatById(id).subscribe({
      next: (boat) => {
        this.boatForm.patchValue({ name: boat.name, description: boat.description });
        this.loadingData = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.snackBar.open('Failed to load boat', 'Close', { duration: 3000, panelClass: 'error-snackbar' });
        this.router.navigate(['/boats']);
      }
    });
  }

  onSubmit(): void {
    if (this.boatForm.invalid) return;

    this.saving = true;
    this.cdr.detectChanges();
    const request = this.boatForm.value;

    const operation = this.isEditMode
      ? this.boatService.updateBoat(this.boatId!, request)
      : this.boatService.createBoat(request);

    operation.subscribe({
      next: (boat) => {
        const msg = this.isEditMode ? 'Boat updated successfully' : 'Boat added successfully';
        this.snackBar.open(msg, 'Close', { duration: 3000, panelClass: 'success-snackbar' });
        this.router.navigate(['/boats', boat.id]).then(navigated => {
          if (!navigated) this.saving = false;
        });
        this.saving = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        const msg = err?.error?.detail || 'Failed to save boat';
        this.snackBar.open(msg, 'Close', { duration: 4000, panelClass: 'error-snackbar' });
        this.saving = false;
        this.cdr.detectChanges();
      }
    });
  }
}
