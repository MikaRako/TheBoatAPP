import { ApplicationConfig, importProvidersFrom, provideAppInitializer, inject } from '@angular/core';
import { provideRouter, withComponentInputBinding, withEnabledBlockingInitialNavigation } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { OAuthModule } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { authInterceptor } from './shared/interceptors/auth.interceptor';
import { AuthService } from './shared/services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    // withEnabledBlockingInitialNavigation ensures the router waits for all
    // APP_INITIALIZERs (including auth token exchange) before activating any
    // route or guard. Without this, the guard runs before the token is ready,
    // sees no valid token, calls initCodeFlow() a second time, producing a
    // duplicate Keycloak redirect and a broken component lifecycle.
    provideRouter(routes, withComponentInputBinding(), withEnabledBlockingInitialNavigation()),
    provideHttpClient(withInterceptors([authInterceptor])),
    importProvidersFrom(OAuthModule.forRoot()),
    provideAppInitializer(() => inject(AuthService).initAuth())
  ]
};
