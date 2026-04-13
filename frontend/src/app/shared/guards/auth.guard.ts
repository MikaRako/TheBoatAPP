import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  if (inject(OAuthService).hasValidAccessToken()) {
    return true;
  }
  inject(AuthService).login(); // redirects to Keycloak
  return false;
};
