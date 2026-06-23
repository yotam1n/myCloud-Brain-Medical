import 'vue-router';

import type { WorkspaceRole } from '@/types/enums';

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    role?: WorkspaceRole;
  }
}
