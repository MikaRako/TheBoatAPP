import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  if (inject(OAuthService).hasValidAccessToken()) {
    return true;
  }
  inject(AuthService).login(state.url); // redirects to Keycloak, preserving return URL
  return false;
};
