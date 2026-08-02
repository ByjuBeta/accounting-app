import { OrganizationSwitcher } from './OrganizationSwitcher'

export function Topbar() {
  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-slate-200 bg-white px-6">
      <div />
      <OrganizationSwitcher />
    </header>
  )
}
