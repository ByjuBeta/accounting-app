export interface FieldError {
  field: string
  message: string
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  errorCode: string
  message: string
  path: string
  correlationId: string | null
  fieldErrors: FieldError[]
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
