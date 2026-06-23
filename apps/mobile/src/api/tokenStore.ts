let accessToken: string | null = null;
let refreshToken: string | null = null;

export const tokenStore = {
  getAccessToken() {
    return accessToken;
  },
  getRefreshToken() {
    return refreshToken;
  },
  setTokens(access: string, refresh: string) {
    accessToken = access;
    refreshToken = refresh;
  },
  clear() {
    accessToken = null;
    refreshToken = null;
  },
};
