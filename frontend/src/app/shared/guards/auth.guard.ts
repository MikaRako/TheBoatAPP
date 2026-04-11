import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

// This guard ONLY gates access — it never initiates a new auth flow.
// Auth redirection is handled exclusively in AuthService.initAuth(),
// which runs inside provideAppInitializer (blocking navigation) and
// calls initCodeFlow() at most once per page load.
// Calling initCodeFlow() here would restart the loop on every guard check.
export const authGuard: CanActivateFn = () => {
  return inject(OAuthService).hasValidAccessToken();
};
