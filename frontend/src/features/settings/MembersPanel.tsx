import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Table, THead, TBody, TR, TH, TD } from '@/components/ui/Table'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Field } from '@/components/ui/Field'
import { Skeleton } from '@/components/ui/Skeleton'
import { toast } from '@/store/useToastStore'
import { ApiError } from '@/api/client'
import { membersApi, type Member } from '@/api/members'
import type { Role } from '@/api/auth'
import { useAuthStore } from '@/store/useAuthStore'

const ROLE_OPTIONS: Role[] = ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER', 'LIMITED_USER']

const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  MANAGER: 'Manager',
  ACCOUNTANT: 'Accountant',
  VIEWER: 'Viewer',
  LIMITED_USER: 'Limited User',
}

interface MembersPanelProps {
  canManage: boolean
}

export function MembersPanel({ canManage }: MembersPanelProps) {
  const queryClient = useQueryClient()
  const currentUserId = useAuthStore((s) => s.user?.id)
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>('ACCOUNTANT')

  const { data: members, isLoading } = useQuery({
    queryKey: ['members'],
    queryFn: membersApi.list,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['members'] })

  const addMutation = useMutation({
    mutationFn: () => membersApi.add({ email: email.trim(), role }),
    onSuccess: () => {
      invalidate()
      toast.success('Member added')
      setEmail('')
    },
    onError: (error) => {
      toast.error('Could not add member', error instanceof ApiError ? error.message : undefined)
    },
  })

  const updateRoleMutation = useMutation({
    mutationFn: ({ id, newRole }: { id: string; newRole: Role }) => membersApi.updateRole(id, newRole),
    onSuccess: () => {
      invalidate()
      toast.success('Role updated')
    },
    onError: (error) => {
      toast.error('Could not update role', error instanceof ApiError ? error.message : undefined)
    },
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => membersApi.remove(id),
    onSuccess: () => {
      invalidate()
      toast.success('Member removed')
    },
    onError: (error) => {
      toast.error('Could not remove member', error instanceof ApiError ? error.message : undefined)
    },
  })

  if (isLoading || !members) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <Table>
        <THead>
          <TR>
            <TH>Name</TH>
            <TH>Email</TH>
            <TH>Role</TH>
            {canManage && <TH className="text-right">Actions</TH>}
          </TR>
        </THead>
        <TBody>
          {members.map((member: Member) => {
            const isSelf = member.userId === currentUserId
            return (
              <TR key={member.id}>
                <TD className="font-medium text-slate-900">
                  {member.firstName} {member.lastName}
                  {isSelf && <span className="ml-2 text-xs text-slate-400">(you)</span>}
                </TD>
                <TD>{member.email}</TD>
                <TD>
                  {canManage ? (
                    <Select
                      value={member.role}
                      onChange={(e) =>
                        updateRoleMutation.mutate({ id: member.id, newRole: e.target.value as Role })
                      }
                      className="w-40"
                    >
                      {ROLE_OPTIONS.map((r) => (
                        <option key={r} value={r}>
                          {ROLE_LABELS[r]}
                        </option>
                      ))}
                    </Select>
                  ) : (
                    <Badge tone="slate">{ROLE_LABELS[member.role]}</Badge>
                  )}
                </TD>
                {canManage && (
                  <TD className="text-right">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => removeMutation.mutate(member.id)}
                      disabled={isSelf || removeMutation.isPending}
                    >
                      Remove
                    </Button>
                  </TD>
                )}
              </TR>
            )
          })}
        </TBody>
      </Table>

      {canManage && (
        <form
          className="flex items-end gap-2"
          onSubmit={(e) => {
            e.preventDefault()
            if (!email.trim()) return
            addMutation.mutate()
          }}
        >
          <Field label="Add member by email" htmlFor="memberEmail" className="flex-1">
            <Input
              id="memberEmail"
              type="email"
              placeholder="colleague@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
          <Field label="Role" htmlFor="memberRole" className="w-40">
            <Select id="memberRole" value={role} onChange={(e) => setRole(e.target.value as Role)}>
              {ROLE_OPTIONS.map((r) => (
                <option key={r} value={r}>
                  {ROLE_LABELS[r]}
                </option>
              ))}
            </Select>
          </Field>
          <Button type="submit" disabled={addMutation.isPending}>
            Add
          </Button>
        </form>
      )}
      {!canManage && <p className="text-xs text-slate-500">Only admins can add members or change roles.</p>}
    </div>
  )
}
