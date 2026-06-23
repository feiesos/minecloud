import { Platform } from 'react-native';

// Android emulator uses 10.0.2.2 to reach host machine
// iOS simulator / web can use localhost
const HOST = Platform.select({
  android: '192.168.251.155',
  default: '192.168.251.155',
});

export const API_BASE_URL = `http://${HOST}:8080/api/v1`;
