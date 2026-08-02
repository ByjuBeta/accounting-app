import type { Account } from '@/api/accounts'

export interface AccountTreeNode extends Account {
  children: AccountTreeNode[]
  depth: number
}

/** Builds a parent/child tree from the flat account list, then flattens it back into
 * depth-first order so it can be rendered as a plain (indented) table. */
export function buildAccountTree(accounts: Account[]): AccountTreeNode[] {
  const byId = new Map<string, AccountTreeNode>()
  for (const account of accounts) {
    byId.set(account.id, { ...account, children: [], depth: 0 })
  }

  const roots: AccountTreeNode[] = []
  for (const node of byId.values()) {
    if (node.parentId && byId.has(node.parentId)) {
      byId.get(node.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  }

  const sortByCode = (nodes: AccountTreeNode[]) => nodes.sort((a, b) => a.code.localeCompare(b.code))
  const assignDepth = (nodes: AccountTreeNode[], depth: number) => {
    sortByCode(nodes)
    for (const node of nodes) {
      node.depth = depth
      assignDepth(node.children, depth + 1)
    }
  }
  assignDepth(roots, 0)

  const flatten = (nodes: AccountTreeNode[]): AccountTreeNode[] =>
    nodes.flatMap((node) => [node, ...flatten(node.children)])

  return flatten(roots)
}

/** IDs of a node and everything beneath it — used to keep an account from being reparented under itself. */
export function descendantIds(accounts: Account[], rootId: string): Set<string> {
  const childrenByParent = new Map<string, Account[]>()
  for (const account of accounts) {
    if (!account.parentId) continue
    const list = childrenByParent.get(account.parentId) ?? []
    list.push(account)
    childrenByParent.set(account.parentId, list)
  }

  const result = new Set<string>()
  const stack = [rootId]
  while (stack.length > 0) {
    const current = stack.pop()!
    for (const child of childrenByParent.get(current) ?? []) {
      if (!result.has(child.id)) {
        result.add(child.id)
        stack.push(child.id)
      }
    }
  }
  return result
}
