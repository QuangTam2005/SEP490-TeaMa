'use client';

import { useEffect, useState, type ReactNode } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CheckCircle2,
  KeyRound,
  Link2,
  Loader2,
  Lock,
  Save,
  Unlink,
  UserCircle,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { GuardianShell } from '@/components/guardian/guardian-shell';
import { StudentShell } from '@/components/student/student-shell';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { GRADE_OPTIONS } from '@/lib/study-utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { IS_MOCK_MODE, authService, linkService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import type { UserRole } from '@/types';

export function ProfileView() {
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();
  const user = useAuthStore((state) => state.user);
  const syncUser = useAuthStore((state) => state.syncUser);

  const activeRole: UserRole = IS_MOCK_MODE
    ? runtime.simulatedRole
    : user?.role ?? 'STUDENT';

  const [fullName, setFullName] = useState(user?.fullName ?? '');
  const [grade, setGrade] = useState<number>(user?.grade ?? 6);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [confirmUnlinkId, setConfirmUnlinkId] = useState<number | 'SELF' | null>(
    null
  );

  useEffect(() => {
    if (user) {
      setFullName(user.fullName);
      if (user.grade) {
        setGrade(user.grade);
      }
    }
  }, [user]);

  const studentLinkQuery = useQuery({
    queryKey: [
      'profile-student-link',
      activeRole,
      runtime.resetCounter,
      runtime.scenario,
    ],
    queryFn: () => linkService.getCurrentLink(),
    enabled: activeRole === 'STUDENT',
  });

  const childrenQuery = useQuery({
    queryKey: [
      'linked-children',
      activeRole,
      runtime.resetCounter,
      runtime.scenario,
    ],
    queryFn: () => linkService.getLinkedChildren(),
    enabled: activeRole === 'GUARDIAN',
  });

  const profileMutation = useMutation({
    mutationFn: () =>
      authService.updateProfile({
        fullName: fullName.trim(),
        grade: activeRole === 'STUDENT' ? grade : null,
      }),
    onSuccess: (updated) => {
      syncUser(updated);
      void queryClient.invalidateQueries({ queryKey: ['subjects'] });
      void queryClient.invalidateQueries({ queryKey: ['student-timetable'] });
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      toast.success('Đã cập nhật thông tin tài khoản');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const passwordMutation = useMutation({
    mutationFn: () =>
      authService.changePassword({
        currentPassword,
        newPassword,
      }),
    onSuccess: () => {
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordError(null);
      toast.success('Đã đổi mật khẩu thành công');
    },
    onError: (err) => {
      const msg = extractApiErrorMessage(err);
      setPasswordError(msg);
      toast.error(msg);
    },
  });

  const unlinkMutation = useMutation({
    mutationFn: (studentId?: number) => linkService.unlink(studentId),
    onSuccess: async () => {
      setConfirmUnlinkId(null);
      await queryClient.invalidateQueries({ queryKey: ['profile-student-link'] });
      await queryClient.invalidateQueries({ queryKey: ['linked-children'] });
      await queryClient.invalidateQueries({ queryKey: ['student-link-code'] });
      await queryClient.invalidateQueries({ queryKey: ['guardian-dashboard'] });
      try {
        const refreshedUser = await authService.getCurrentUser();
        syncUser(refreshedUser);
      } catch {
        // Ignore silent refresh error
      }
      toast.success('Đã ngắt liên kết tài khoản');
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const handleProfileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (fullName.trim().length < 2) {
      toast.error('Họ và tên phải có ít nhất 2 ký tự');
      return;
    }
    profileMutation.mutate();
  };

  const handlePasswordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);
    if (!currentPassword) {
      setPasswordError('Vui lòng nhập mật khẩu hiện tại');
      return;
    }
    if (newPassword.length < 6) {
      setPasswordError('Mật khẩu mới phải có ít nhất 6 ký tự');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Mật khẩu xác nhận không khớp');
      return;
    }
    passwordMutation.mutate();
  };

  const wrapWithShell = (content: ReactNode) =>
    activeRole === 'GUARDIAN' ? (
      <GuardianShell>{content}</GuardianShell>
    ) : (
      <StudentShell>{content}</StudentShell>
    );

  return wrapWithShell(
    <div className="mx-auto w-full max-w-[1440px] space-y-6">
      <div className="flex flex-col gap-3 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <UserCircle
              className="h-6 w-6 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
              Hồ sơ & Bảo mật tài khoản
            </h1>
          </div>
          <p className="text-sm text-muted-foreground">
            Cập nhật thông tin cá nhân, mật khẩu và quản lý liên kết Phụ huynh – Học sinh.
          </p>
        </div>
        <Badge variant="brand">
          {activeRole === 'GUARDIAN' ? 'Phụ huynh' : `Học sinh · Lớp ${grade}`}
        </Badge>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <div className="space-y-6 xl:col-span-7">
      {/* Card 1: Personal profile */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <UserCircle className="h-5 w-5 text-brand-strong" strokeWidth={1.75} />
            Thông tin cá nhân
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleProfileSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="profile-email">Email đăng nhập</Label>
              <Input
                id="profile-email"
                value={user?.email ?? ''}
                disabled
                className="h-11 bg-muted/50"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="profile-fullname">Họ và tên</Label>
              <Input
                id="profile-fullname"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nhập họ và tên"
                className="h-11"
                maxLength={100}
                required
              />
            </div>

            {activeRole === 'STUDENT' ? (
              <div className="space-y-1.5">
                <Label htmlFor="profile-grade">Khối lớp hiện tại</Label>
                <select
                  id="profile-grade"
                  value={grade}
                  onChange={(e) => setGrade(Number(e.target.value))}
                  className="flex h-11 w-full rounded-lg border bg-background px-3 text-sm font-medium text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {GRADE_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
            ) : null}

            <div className="flex justify-end">
              <Button
                type="submit"
                className="h-11 gap-2 px-5 font-semibold"
                disabled={profileMutation.isPending}
              >
                {profileMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" strokeWidth={1.75} />
                )}
                Lưu thông tin
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Card 2: Change password */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Lock className="h-5 w-5 text-brand-strong" strokeWidth={1.75} />
            Đổi mật khẩu
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="current-password">Mật khẩu hiện tại</Label>
              <Input
                id="current-password"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="h-11"
                autoComplete="current-password"
              />
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="new-password">Mật khẩu mới</Label>
                <Input
                  id="new-password"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Tối thiểu 6 ký tự"
                  className="h-11"
                  autoComplete="new-password"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="confirm-password">Nhập lại mật khẩu mới</Label>
                <Input
                  id="confirm-password"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="h-11"
                  autoComplete="new-password"
                />
              </div>
            </div>

            {passwordError ? (
              <p className="text-sm font-medium text-rose-600 dark:text-rose-400">
                {passwordError}
              </p>
            ) : null}

            <div className="flex justify-end">
              <Button
                type="submit"
                variant="outline"
                className="h-11 gap-2 px-5 font-semibold"
                disabled={passwordMutation.isPending}
              >
                {passwordMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <KeyRound className="h-4 w-4" strokeWidth={1.75} />
                )}
                Cập nhật mật khẩu
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Card 3: Link Management */}
        </div>
        <div className="xl:col-span-5">
      <Card>
        <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <CardTitle className="flex items-center gap-2 text-lg">
            <Link2 className="h-5 w-5 text-brand-strong" strokeWidth={1.75} />
            {activeRole === 'GUARDIAN'
              ? 'Danh sách học sinh đang liên kết'
              : 'Liên kết với Phụ huynh'}
          </CardTitle>
          <Link
            href="/link-student"
            className="inline-flex h-10 items-center gap-2 rounded-lg border bg-background px-3.5 text-xs font-semibold text-foreground hover:bg-muted"
          >
            <KeyRound className="h-3.5 w-3.5 text-brand-strong" />
            {activeRole === 'GUARDIAN'
              ? '+ Thêm học sinh'
              : 'Tạo mã liên kết mới'}
          </Link>
        </CardHeader>
        <CardContent className="space-y-3">
          {activeRole === 'STUDENT' ? (
            studentLinkQuery.isLoading ? (
              <p className="text-sm text-muted-foreground">Đang tải trạng thái liên kết...</p>
            ) : studentLinkQuery.data?.status === 'ACTIVE' ? (
              <div className="flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-2.5">
                  <CheckCircle2 className="h-5 w-5 text-emerald-600" />
                  <div>
                    <p className="text-sm font-semibold text-foreground">
                      Đang liên kết với Phụ huynh:{' '}
                      {studentLinkQuery.data.linkedUserName ?? 'Phụ huynh'}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Phụ huynh có thể xem tiến độ và thời khóa biểu của con.
                    </p>
                  </div>
                </div>
                {confirmUnlinkId === 'SELF' ? (
                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      variant="destructive"
                      size="sm"
                      disabled={unlinkMutation.isPending}
                      onClick={() => unlinkMutation.mutate(undefined)}
                    >
                      Xác nhận ngắt
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setConfirmUnlinkId(null)}
                    >
                      Hủy
                    </Button>
                  </div>
                ) : (
                  <Button
                    type="button"
                    variant="outline"
                    className="h-10 gap-1.5 text-rose-600 hover:text-rose-700"
                    onClick={() => setConfirmUnlinkId('SELF')}
                  >
                    <Unlink className="h-4 w-4" />
                    Ngắt liên kết
                  </Button>
                )}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                Hiện chưa có tài khoản Phụ huynh nào đang liên kết. Bạn có thể bấm &ldquo;Tạo mã liên kết mới&rdquo; để chia sẻ mã 8 ký tự cho bố mẹ.
              </p>
            )
          ) : childrenQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">Đang tải danh sách con...</p>
          ) : !childrenQuery.data || childrenQuery.data.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Chưa liên kết với học sinh nào. Bấm &ldquo;+ Thêm học sinh&rdquo; để nhập mã 8 ký tự từ tài khoản của con.
            </p>
          ) : (
            <div className="space-y-2.5">
              {childrenQuery.data.map((child) => (
                <div
                  key={child.studentId}
                  className="flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="text-sm font-semibold text-foreground">
                      {child.displayName}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Lớp {child.grade}
                    </p>
                  </div>
                  {confirmUnlinkId === child.studentId ? (
                    <div className="flex items-center gap-2">
                      <Button
                        type="button"
                        variant="destructive"
                        size="sm"
                        disabled={unlinkMutation.isPending}
                        onClick={() => unlinkMutation.mutate(child.studentId)}
                      >
                        Xác nhận ngắt
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => setConfirmUnlinkId(null)}
                      >
                        Hủy
                      </Button>
                    </div>
                  ) : (
                    <Button
                      type="button"
                      variant="outline"
                      className="h-10 gap-1.5 text-rose-600 hover:text-rose-700"
                      onClick={() => setConfirmUnlinkId(child.studentId)}
                    >
                      <Unlink className="h-4 w-4" />
                      Ngắt liên kết
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
        </div>
      </div>
    </div>
  );
}
