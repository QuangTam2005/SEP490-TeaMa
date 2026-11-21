'use client';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, CheckCircle2, Loader2, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  applyApiFieldErrors,
  extractApiErrorMessage,
} from '@/components/shared/form-error-utils';
import { projectService } from '@/services';

const createProjectSchema = z.object({
  projectCode: z
    .string()
    .trim()
    .min(2, 'Mã dự án phải có ít nhất 2 ký tự')
    .max(30, 'Mã dự án tối đa 30 ký tự'),
  projectName: z
    .string()
    .trim()
    .min(3, 'Tên dự án phải có ít nhất 3 ký tự')
    .max(255, 'Tên dự án tối đa 255 ký tự'),
  groupName: z
    .string()
    .trim()
    .min(2, 'Tên nhóm phải có ít nhất 2 ký tự')
    .max(50, 'Tên nhóm tối đa 50 ký tự'),
  softwareType: z.string().trim().max(100, 'Tối đa 100 ký tự').optional(),
  startDate: z.string().min(1, 'Vui lòng chọn ngày bắt đầu Tuần 1'),
  supervisorName: z.string().trim().max(150, 'Tối đa 150 ký tự').optional(),
  supervisorEmail: z
    .string()
    .trim()
    .email('Email giảng viên không đúng định dạng')
    .or(z.literal(''))
    .optional(),
});

type CreateProjectFormValues = z.infer<typeof createProjectSchema>;

export interface CreateProjectDialogProps {
  disabled?: boolean;
  triggerLabel?: string;
}

export function CreateProjectDialog({
  disabled = false,
  triggerLabel = 'Tạo dự án mới',
}: CreateProjectDialogProps) {
  const [open, setOpen] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<CreateProjectFormValues>({
    resolver: zodResolver(createProjectSchema),
    defaultValues: {
      projectCode: '',
      projectName: '',
      groupName: '',
      softwareType: 'Web Application',
      startDate: new Date().toISOString().slice(0, 10),
      supervisorName: '',
      supervisorEmail: '',
    },
  });

  const mutation = useMutation({
    mutationFn: (values: CreateProjectFormValues) =>
      projectService.createProject({
        projectCode: values.projectCode,
        projectName: values.projectName,
        groupName: values.groupName,
        softwareType: values.softwareType || undefined,
        startDate: values.startDate,
        supervisorName: values.supervisorName || undefined,
        supervisorEmail: values.supervisorEmail || undefined,
      }),
    onSuccess: (created) => {
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      toast.success(
        `Đã khởi tạo dự án ${created.projectCode} kèm 10 Milestones & 5 Quality Objectives`
      );
      setSubmitError(null);
      reset();
      setOpen(false);
    },
    onError: (err) => {
      const mapped = applyApiFieldErrors(
        err,
        setError,
        new Set([
          'projectCode',
          'projectName',
          'groupName',
          'softwareType',
          'startDate',
          'supervisorName',
          'supervisorEmail',
        ])
      );
      const message = extractApiErrorMessage(err);
      if (!mapped) {
        setSubmitError(message);
      }
      toast.error(message);
    },
  });

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) {
          setSubmitError(null);
        }
      }}
    >
      <DialogTrigger asChild>
        <Button type="button" disabled={disabled} className="h-9 px-4">
          <Plus className="h-4 w-4" strokeWidth={1.75} />
          {triggerLabel}
        </Button>
      </DialogTrigger>

      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Khởi tạo dự án SEP490</DialogTitle>
          <DialogDescription>
            Bạn sẽ tự động trở thành Nhóm trưởng (Leader) và có thể mời thành
            viên qua Email ngay sau khi tạo.
          </DialogDescription>
        </DialogHeader>

        <div className="flex items-start gap-2 rounded-md border bg-brand-soft px-3.5 py-2.5 text-xs text-foreground">
          <CheckCircle2
            className="mt-0.5 h-4 w-4 shrink-0 text-brand"
            strokeWidth={1.75}
          />
          <span>
            Hệ thống tự động khởi tạo{' '}
            <strong>10 cột mốc SEP490 (Report 1–7)</strong>, danh mục nộp bài
            lũy tiến và <strong>5 Quality Objectives</strong>.
          </span>
        </div>

        {submitError ? (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-md border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-xs text-rose-700 dark:border-rose-900 dark:bg-rose-950/60 dark:text-rose-300"
          >
            <AlertCircle
              className="mt-0.5 h-4 w-4 shrink-0"
              strokeWidth={1.75}
            />
            <span>{submitError}</span>
          </div>
        ) : null}

        <form
          onSubmit={handleSubmit((values) => mutation.mutate(values))}
          className="space-y-3.5"
          noValidate
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="proj-code">Mã dự án</Label>
              <Input
                id="proj-code"
                placeholder="SEP490-G12"
                className="font-mono"
                {...register('projectCode')}
              />
              {errors.projectCode ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">
                  {errors.projectCode.message}
                </p>
              ) : null}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="proj-group">Tên nhóm</Label>
              <Input
                id="proj-group"
                placeholder="G12-SE18"
                {...register('groupName')}
              />
              {errors.groupName ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">
                  {errors.groupName.message}
                </p>
              ) : null}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="proj-name">Tên đề tài đồ án</Label>
            <Input
              id="proj-name"
              placeholder="SEP490 Capstone Copilot"
              {...register('projectName')}
            />
            {errors.projectName ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">
                {errors.projectName.message}
              </p>
            ) : null}
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="proj-type">Loại phần mềm</Label>
              <Input
                id="proj-type"
                placeholder="Web Application / Mobile"
                {...register('softwareType')}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="proj-start">Ngày bắt đầu (Tuần 1)</Label>
              <Input
                id="proj-start"
                type="date"
                className="font-mono"
                {...register('startDate')}
              />
              {errors.startDate ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">
                  {errors.startDate.message}
                </p>
              ) : null}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="proj-sup-name">Giảng viên hướng dẫn</Label>
              <Input
                id="proj-sup-name"
                placeholder="TS. Nguyễn Văn Hùng"
                {...register('supervisorName')}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="proj-sup-email">Email GVHD (tuỳ chọn)</Label>
              <Input
                id="proj-sup-email"
                type="email"
                placeholder="mentor@example.com"
                {...register('supervisorEmail')}
              />
              {errors.supervisorEmail ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">
                  {errors.supervisorEmail.message}
                </p>
              ) : null}
            </div>
          </div>

          <DialogFooter className="pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
            >
              Hủy
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? (
                <>
                  <Loader2
                    className="h-4 w-4 animate-spin"
                    strokeWidth={1.75}
                  />
                  Đang khởi tạo...
                </>
              ) : (
                'Tạo dự án'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
