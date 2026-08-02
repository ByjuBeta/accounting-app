import { useNavigate } from 'react-router-dom'
import { OrganizationSwitcher } from './OrganizationSwitcher'
import { Button } from '@/components/ui/Button'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/useAuthStore'

export function Topbar() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const clear = useAuthStore((s) => s.clear)

  async function handleLogout() {
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken)
      } catch {
        // best-effort — proceed with clearing local state regardless
      }
    }
    clear()
    navigate('/login', { replace: true })
  }

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-slate-200 bg-white px-6">
      <div />
      <div className="flex items-center gap-4">
        <OrganizationSwitcher />
        {user && (
          <div className="flex items-center gap-2 border-l border-slate-200 pl-4">
            <span className="text-sm text-slate-600">
              {user.firstName} {user.lastName}
            </span>
            <Button type="button" variant="ghost" size="sm" onClick={handleLogout}>
              Log out
            </Button>
          </div>
        )}
      </div>
    </header>
  )
}
