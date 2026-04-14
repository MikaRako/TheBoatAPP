import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';
import { BoatService } from '../../shared/services/boat.service';
import { AuthService } from '../../shared/services/auth.service';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';

@Component({
  selector: 'app-boat-form',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="page-container">
      <nav class="breadcrumb" aria-label="Breadcrumb">
        <ol>
          <li><a routerLink="/boats">Fleet</a></li>
          <li>
            <mat-icon aria-hidden="true">chevron_right</mat-icon>
            @if (isEditMode) {
              <span aria-current="page">Edit Boat</span>
            } @else {
              <span aria-current="page">Add Boat</span>
            }
          </li>
        </ol>
      </nav>

      @if (loadingData) {
        <app-loading-spinner message="Loading..." />
      }

      @if (!loadingData) {
        <mat-card class="form-card">
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
              <input matInput formControlName="name" placeholder="e.g. Sea Explorer" maxlength="40" aria-required="true" />
              <mat-hint align="end">{{ boatForm.get('name')?.value?.length || 0 }}/40</mat-hint>
              @if (boatForm.get('name')?.hasError('required'))  { <mat-error>Name is required</mat-error> }
              @if (boatForm.get('name')?.hasError('minlength')) { <mat-error>Name must be at least 1 character</mat-error> }
              @if (boatForm.get('name')?.hasError('maxlength')) { <mat-error>Name must not exceed 40 characters</mat-error> }
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
              @if (boatForm.get('description')?.hasError('maxlength')) { <mat-error>Description must not exceed 2000 characters</mat-error> }
            </mat-form-field>

            <div class="two-col">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Status *</mat-label>
                <mat-icon matPrefix>radio_button_checked</mat-icon>
                <mat-select formControlName="status" aria-required="true">
                  <mat-option value="UNDERWAY">Underway</mat-option>
                  <mat-option value="IN_PORT">In Port</mat-option>
                  <mat-option value="MAINTENANCE">Maintenance</mat-option>
                </mat-select>
                @if (boatForm.get('status')?.hasError('required')) { <mat-error>Status is required</mat-error> }
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Vessel Type *</mat-label>
                <mat-icon matPrefix>directions_boat</mat-icon>
                <mat-select formControlName="type" aria-required="true">
                  <mat-option value="SAILBOAT">Sailboat</mat-option>
                  <mat-option value="TRAWLER">Trawler</mat-option>
                  <mat-option value="CARGO_SHIP">Cargo Ship</mat-option>
                  <mat-option value="YACHT">Yacht</mat-option>
                  <mat-option value="FERRY">Ferry</mat-option>
                </mat-select>
                @if (boatForm.get('type')?.hasError('required')) { <mat-error>Vessel type is required</mat-error> }
              </mat-form-field>
            </div>

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
      }
    </div>
  `,
  styles: [`
    .page-container {
      max-width: 900px;
      margin: 0 auto;
      padding: 28px 24px;
    }
    .breadcrumb {
      margin-bottom: 24px;
      color: var(--color-text-secondary);
      font-size: 0.9rem;
      ol {
        display: flex;
        align-items: center;
        gap: 4px;
        list-style: none;
        padding: 0;
        margin: 0;
      }
      li { display: flex; align-items: center; gap: 4px; }
      a { color: var(--color-heading); text-decoration: none; }
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
      color: var(--color-heading);
    }
    p { color: var(--color-text-secondary); font-size: 0.9rem; margin-top: 4px; }
    .boat-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .full-width { width: 100%; }
    .two-col {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }
    @media (max-width: 600px) {
      .two-col { grid-template-columns: 1fr; }
    }
    .form-actions {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
      margin-top: 8px;
    }
  `]
})
export class BoatFormComponent implements OnInit, OnDestroy {
  boatForm!: FormGroup;
  isEditMode = false;
  boatId: number | null = null;
  loadingData = false;
  saving = false;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private boatService: BoatService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.boatForm = this.fb.group({
      name:        ['', [Validators.required, Validators.minLength(1), Validators.maxLength(40)]],
      description: ['', [Validators.maxLength(2000)]],
      status:      ['IN_PORT', [Validators.required]],
      type:        ['YACHT',   [Validators.required]]
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.boatId     = Number(id);
      this.loadBoat(this.boatId);
    }

    // Reload form data if a token refresh happens while the form is open
    this.authService.onTokenReceived$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.isEditMode && this.boatId) this.loadBoat(this.boatId);
    });
  }

  private loadBoat(id: number): void {
    this.loadingData = true;
    this.boatService.getBoatById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (boat) => {
        this.boatForm.patchValue({
          name: boat.name, description: boat.description,
          status: boat.status, type: boat.type
        });
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
    const request = this.boatForm.value;

    const operation = this.isEditMode
      ? this.boatService.updateBoat(this.boatId!, request)
      : this.boatService.createBoat(request);

    operation.pipe(takeUntil(this.destroy$)).subscribe({
      next: (boat) => {
        this.snackBar.open(
          this.isEditMode ? 'Boat updated successfully' : 'Boat added successfully',
          'Close', { duration: 3000, panelClass: 'success-snackbar' }
        );
        this.router.navigate(['/boats', boat.id]);
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.detail || 'Failed to save boat',
          'Close', { duration: 4000, panelClass: 'error-snackbar' }
        );
        this.saving = false;
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
