import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AppComponent } from './app.component';
import { AuthService } from './shared/services/auth.service';

describe('AppComponent', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'isAuthenticated',
      'userName',
      'logout',
    ]);
    authServiceSpy.isAuthenticated.and.returnValue(false);
    authServiceSpy.userName.and.returnValue('');

    await TestBed.configureTestingModule({
      imports: [AppComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should hide the nav bar when not authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const nav = fixture.nativeElement.querySelector('nav.top-nav');
    expect(nav).toBeNull();
  });

  it('should show the nav bar when authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.userName.and.returnValue('testuser');
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const nav = fixture.nativeElement.querySelector('nav.top-nav');
    expect(nav).not.toBeNull();
  });

  it('should display the username in the toolbar', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.userName.and.returnValue('captain');
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const content: string = fixture.nativeElement.textContent;
    expect(content).toContain('captain');
  });
});
