import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let oauthSpy: jasmine.SpyObj<OAuthService>;

  beforeEach(() => {
    oauthSpy = jasmine.createSpyObj('OAuthService', ['hasValidAccessToken']);

    TestBed.configureTestingModule({
      providers: [{ provide: OAuthService, useValue: oauthSpy }],
    });
  });

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

  it('should allow navigation when a valid token exists', () => {
    oauthSpy.hasValidAccessToken.and.returnValue(true);
    expect(runGuard()).toBeTrue();
  });

  it('should block navigation when no valid token exists', () => {
    oauthSpy.hasValidAccessToken.and.returnValue(false);
    expect(runGuard()).toBeFalse();
  });
});
