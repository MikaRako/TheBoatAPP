import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const oauthService = inject(OAuthService);
  const authService  = inject(AuthService);
  const router       = inject(Router);

  const token = oauthService.getAccessToken();

  if (token && !req.url.includes('/realms/')) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.login(); // token expired — redirect to Keycloak
      } else if (error.status === 403) {
        router.navigate(['/boats']);
      }
      return throwError(() => error);
    })
  );
};
