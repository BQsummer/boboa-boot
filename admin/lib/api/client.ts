/**
 * API 客户端配置
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://8.155.167.155:6053';

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public response?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface FetchOptions extends RequestInit {
  headers?: Record<string, string>;
}

export async function fetchApi<T>(
  endpoint: string,
  options: FetchOptions = {}
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  
  // 从 localStorage 获取 token
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token && !options.headers?.['skip-auth']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorData = await parseErrorResponse(response);
    
    // 处理 401 未授权错误 - token 过期或无效
    if (response.status === 401 && typeof window !== 'undefined') {
      // 清除本地存储的认证信息
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('tokenExpiry');
      localStorage.removeItem('user');
      
      // 跳转到登录页面
      window.location.href = '/login';
    }
    
    throw new ApiError(
      response.status,
      errorData.message || `Request failed with status ${response.status}`,
      errorData
    );
  }

  return parseSuccessResponse<T>(response);
}

async function parseErrorResponse(response: Response): Promise<any> {
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    return response.json().catch(() => ({}));
  }

  const text = await response.text().catch(() => '');
  return text ? { message: text } : {};
}

async function parseSuccessResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') || '';
  const contentLength = response.headers.get('content-length');

  if (contentLength === '0') {
    return undefined as T;
  }

  if (contentType.includes('application/json')) {
    return response.json();
  }

  const text = await response.text();
  return (text as unknown) as T;
}
