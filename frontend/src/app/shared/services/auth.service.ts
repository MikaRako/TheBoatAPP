import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _isAuthenticated = signal(false);
  private _userName = signal('');

  constructor(
    private oauthService: OAuthService,
    private router: Router
  ) {}

  isAuthenticated() {
    return this._isAuthenticated();
  }

  userName() {
    return this._userName();
  }

  getAccessToken(): string | null {
    return this.oauthService.getAccessToken();
  }

  async initAuth(): Promise<void> {
    const authConfig: AuthConfig = {
      issuer: environment.keycloak.issuer,
      redirectUri: environment.keycloak.redirectUri,
      clientId: environment.keycloak.clientId,
      scope: environment.keycloak.scope,
      responseType: environment.keycloak.responseType,
      showDebugInformation: environment.keycloak.showDebugInformation,
      clearHashAfterLogin: true,
      requireHttps: false,
    };

    this.oauthService.configure(authConfig);
    try {
      // Load discovery document first, then attempt the appropriate login flow.
      await this.oauthService.loadDiscoveryDocument();

      // For authorization code flow the helper methods may not return a boolean
      // (they can be void/undefined). Don't rely on a return value — call the
      // appropriate method and then check `hasValidAccessToken()`.
      if (environment.keycloak.responseType === 'code' && typeof (this.oauthService as any).tryLoginCodeFlow === 'function') {
        await (this.oauthService as any).tryLoginCodeFlow();
      } else {
        await (this.oauthService as any).tryLogin();
      }

      const hasToken = this.oauthService.hasValidAccessToken();
      this._isAuthenticated.set(hasToken);

      if (this._isAuthenticated()) {
        const claims = this.oauthService.getIdentityClaims() as any;
        this._userName.set(claims?.['preferred_username'] || claims?.['name'] || 'User');
        this.router.navigate(['/boats']);
        try { window.dispatchEvent(new Event('auth:token_received')); } catch {}
      }
      // Do not auto-redirect to Keycloak — let the auth guard send
      // unauthenticated users to the custom /login page instead.
    } catch (err) {
      console.error('Auth init error:', err);
      // Do not auto-redirect on error — user will reach /login via the guard.
    }

    // Setup silent refresh after initial discovery/config
    try {
      this.oauthService.setupAutomaticSilentRefresh();
    } catch (e) {
      // not critical; some setups may not support silent refresh
      console.warn('Silent refresh not available', e);
    }

    this.oauthService.events.subscribe(event => {
      this._isAuthenticated.set(this.oauthService.hasValidAccessToken());
      if (event.type === 'token_received') {
        const claims = this.oauthService.getIdentityClaims() as any;
        this._userName.set(claims?.['preferred_username'] || 'User');
        try { window.dispatchEvent(new Event('auth:token_received')); } catch {}
      }
      if (event.type === 'session_terminated' || event.type === 'token_expires') {
        this.login();
      }
    });
  }


  login(): void {
    this.oauthService.initCodeFlow();
  }

  register(): void {
    const cfg = this.oauthService as any;
    const issuer: string = cfg.issuer ?? '';
    const clientId: string = cfg.clientId ?? '';
    const redirectUri: string = cfg.redirectUri ?? window.location.origin;
    const scope: string = cfg.scope ?? 'openid';
    const url =
      `${issuer}/protocol/openid-connect/registrations` +
      `?client_id=${encodeURIComponent(clientId)}` +
      `&response_type=code` +
      `&scope=${encodeURIComponent(scope)}` +
      `&redirect_uri=${encodeURIComponent(redirectUri)}`;
    window.location.href = url;
  }

  logout(): void {
    this.oauthService.logOut();
  }
}
