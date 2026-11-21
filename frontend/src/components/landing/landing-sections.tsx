import Link from 'next/link';
import {
  ArrowRight,
  Check,
  CheckCircle2,
  Eye,
  FileSpreadsheet,
  Link2,
  Mail,
  ShieldCheck,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { HeroAppFrame } from '@/components/landing/hero-app-frame';

const OGA_SEGMENTS = [
  { code: 'M01', label: 'R1 Intro', weight: 4 },
  { code: 'M02', label: 'R2 Plan', weight: 8 },
  { code: 'M03', label: 'R3 SRS', weight: 16 },
  { code: 'M04', label: 'R4 Design', weight: 18 },
  { code: 'M05–08', label: 'Iter 1–4 (R5)', weight: 18 },
  { code: 'M09', label: 'R6 SUG', weight: 4 },
  { code: 'M10', label: 'R7 Final', weight: 32 },
];

const AUTH_PREVIEW_ROLES = [
  { role: 'Leader', queryAll: true, add: true, updateAll: true, del: true },
  { role: 'Member', queryAll: true, add: true, updateAll: false, del: false },
  { role: 'Supervisor', queryAll: true, add: false, updateAll: false, del: false },
];

const TRACEABILITY_NODES = [
  { code: 'FE-01', label: 'Auth & Invite' },
  { code: 'UC-03', label: 'Accept Invite' },
  { code: 'SCR-04', label: 'Invite Gate' },
  { code: 'TASK-12', label: 'MailSender' },
  { code: 'TC-08', label: 'Pass (2 ảnh)' },
];

export function LandingSections() {
  return (
    <main id="top" className="pt-16">
      {/* SECTION 1: HERO */}
      <section className="border-b bg-canvas">
        <div className="mx-auto grid max-w-[1280px] grid-cols-1 items-center gap-10 px-4 py-14 sm:px-6 lg:min-h-[calc(100svh-4rem)] lg:grid-cols-12 lg:gap-12 lg:px-10 lg:py-16">
          <div className="lg:col-span-5 animate-in fade-in slide-in-from-bottom-2 duration-500 motion-reduce:animate-none">
            <p className="mb-3 font-mono text-xs font-medium text-brand">
              SEP490 Capstone Copilot • 15 Tuần
            </p>
            <h1 className="text-3xl font-bold tracking-tight text-foreground sm:text-4xl lg:text-[42px] lg:leading-[1.15]">
              Quản trị đồ án SEP490 trong 15 tuần.
            </h1>
            <p className="mt-4 text-base leading-relaxed text-muted-foreground">
              Số hóa 10 cột mốc OGA, phân rã WBS kèm tự tính độ phức tạp, truy
              vết SRS đến Test Evidence và đồng bộ GitHub đa repository.
            </p>

            <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:items-center">
              <Button asChild className="h-11 px-5 text-sm">
                <Link href="/projects">
                  Mở Workspace
                  <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
                </Link>
              </Button>
              <Button variant="outline" asChild className="h-11 px-5 text-sm">
                <Link href="/supervisor/demo-token">
                  <Eye className="h-4 w-4" strokeWidth={1.75} />
                  Xem Cổng Giảng viên
                </Link>
              </Button>
            </div>

            <div className="mt-7 flex flex-wrap items-center gap-x-5 gap-y-2 border-t pt-5 font-mono text-xs text-muted-foreground tabular-nums">
              <span>10 Milestones (50% OGA)</span>
              <span>Upload tối đa 50MB</span>
              <span>Xuất Excel 5 sheets &amp; SRS .md</span>
            </div>
          </div>

          <div className="lg:col-span-7">
            <HeroAppFrame />
          </div>
        </div>
      </section>

      {/* SECTION 2: 10 MILESTONES OGA */}
      <section id="milestones" className="border-b bg-card">
        <div className="mx-auto max-w-[1280px] px-4 py-16 sm:px-6 lg:px-10 lg:py-24">
          <div className="max-w-2xl">
            <p className="font-mono text-xs font-medium text-brand">
              FR-03 &amp; FR-04 • On-Going Assessment
            </p>
            <h2 className="mt-2 text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              10 cột mốc SEP490 và checklist nộp bài lũy tiến.
            </h2>
            <p className="mt-3 text-base text-muted-foreground">
              Khởi tạo dự án tự động sinh đủ 10 mốc từ Tuần 1 đến Tuần 15 kèm
              trọng số điểm OGA và danh mục báo cáo bắt buộc cần nộp theo từng
              đợt.
            </p>
          </div>

          {/* OGA Weight Bar */}
          <div className="mt-10 rounded-lg border bg-canvas p-5">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2 text-xs">
              <span className="font-medium text-foreground">
                Phân bổ trọng số đánh giá liên tục (100% OGA)
              </span>
              <span className="font-mono text-muted-foreground tabular-nums">
                R1 4% • R2 8% • R3 16% • R4 18% • R5 18% • R6 4% • R7 32%
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
              {OGA_SEGMENTS.map((seg) => (
                <div
                  key={seg.code}
                  className="rounded-md border bg-card p-3"
                >
                  <div className="flex items-center justify-between font-mono text-xs">
                    <span className="font-semibold text-foreground">
                      {seg.code}
                    </span>
                    <span className="font-semibold text-brand tabular-nums">
                      {seg.weight}%
                    </span>
                  </div>
                  <p className="mt-1.5 text-xs text-muted-foreground">
                    {seg.label}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 3: WBS, SRS & TRACEABILITY */}
      <section id="wbs-srs" className="border-b bg-canvas">
        <div className="mx-auto max-w-[1280px] px-4 py-16 sm:px-6 lg:px-10 lg:py-24">
          <div className="max-w-2xl">
            <p className="font-mono text-xs font-medium text-brand">
              Report 2, Report 3 &amp; Report 5
            </p>
            <h2 className="mt-2 text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
              Một dữ liệu gốc từ WBS đến đặc tả SRS và kiểm thử.
            </h2>
            <p className="mt-3 text-base text-muted-foreground">
              Loại bỏ tình trạng lệch pha giữa file Excel tracking và tài liệu
              Word. Mọi thay đổi tự động ghi nhận vào Record of Changes.
            </p>
          </div>

          <div className="mt-10 grid grid-cols-1 gap-6 lg:grid-cols-3">
            {/* Card 1: Complexity Auto-Calculator */}
            <div className="flex flex-col justify-between rounded-lg border bg-card p-6">
              <div>
                <h3 className="text-base font-semibold text-foreground">
                  Tự tính độ phức tạp WBS chuẩn bộ môn
                </h3>
                <p className="mt-1.5 text-sm text-muted-foreground">
                  Nhập số Fields và Transactions để phân loại Simple, Medium
                  hoặc Complex kèm tổng man-days theo Work Category.
                </p>
              </div>
              <div className="mt-6 space-y-2 border-t pt-4 font-mono text-xs tabular-nums">
                <div className="flex items-center justify-between py-1">
                  <span className="text-muted-foreground">
                    &le; 7 fields &amp; &le; 3 trans
                  </span>
                  <span className="font-semibold text-emerald-700 dark:text-emerald-300">
                    Simple
                  </span>
                </div>
                <div className="flex items-center justify-between border-t py-1">
                  <span className="text-muted-foreground">
                    &le; 15 fields &amp; &le; 7 trans
                  </span>
                  <span className="font-semibold text-amber-700 dark:text-amber-300">
                    Medium
                  </span>
                </div>
                <div className="flex items-center justify-between border-t py-1">
                  <span className="text-muted-foreground">
                    &gt; 15 fields hoặc &gt; 7 trans
                  </span>
                  <span className="font-semibold text-rose-700 dark:text-rose-300">
                    Complex
                  </span>
                </div>
              </div>
            </div>

            {/* Card 2: ScreenAuthMatrix */}
            <div className="flex flex-col justify-between rounded-lg border bg-card p-6">
              <div>
                <h3 className="text-base font-semibold text-foreground">
                  Ma trận phân quyền 8 mức click-to-toggle
                </h3>
                <p className="mt-1.5 text-sm text-muted-foreground">
                  Cấu hình trực tiếp quyền Query, Add, Update, Delete cho từng
                  System Actor và xuất thẳng bảng Markdown cho Report 3.
                </p>
              </div>
              <div className="mt-6 overflow-x-auto border-t pt-4">
                <table className="w-full text-left font-mono text-[11px]">
                  <thead>
                    <tr className="text-muted-foreground">
                      <th className="pb-2 font-medium">Actor</th>
                      <th className="pb-2 text-center font-medium">Query</th>
                      <th className="pb-2 text-center font-medium">Add</th>
                      <th className="pb-2 text-center font-medium">Edit</th>
                      <th className="pb-2 text-center font-medium">Del</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {AUTH_PREVIEW_ROLES.map((item) => (
                      <tr key={item.role}>
                        <td className="py-1.5 font-sans text-xs font-medium text-foreground">
                          {item.role}
                        </td>
                        {[item.queryAll, item.add, item.updateAll, item.del].map(
                          (allowed, idx) => (
                            <td key={idx} className="py-1.5 text-center">
                              {allowed ? (
                                <Check
                                  className="mx-auto h-3.5 w-3.5 text-brand"
                                  strokeWidth={2}
                                />
                              ) : (
                                <span className="text-muted-foreground">—</span>
                              )}
                            </td>
                          )
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Card 3: Traceability Matrix */}
            <div className="flex flex-col justify-between rounded-lg border bg-card p-6">
              <div>
                <h3 className="text-base font-semibold text-foreground">
                  Truy vết xuyên suốt đến Test Evidence
                </h3>
                <p className="mt-1.5 text-sm text-muted-foreground">
                  Bấm bất kỳ mã định danh nào để mở Drawer kiểm tra chuỗi liên
                  kết từ Feature đến ảnh minh chứng Test Case.
                </p>
              </div>
              <div className="mt-6 space-y-2 border-t pt-4">
                {TRACEABILITY_NODES.map((node, idx) => (
                  <div
                    key={node.code}
                    className="flex items-center justify-between text-xs"
                  >
                    <span className="inline-flex items-center gap-2">
                      <span className="font-mono text-[11px] text-muted-foreground tabular-nums">
                        0{idx + 1}
                      </span>
                      <span className="rounded border bg-muted px-1.5 py-0.5 font-mono text-[11px] font-semibold text-foreground">
                        {node.code}
                      </span>
                    </span>
                    <span className="text-muted-foreground">{node.label}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 4: EMAIL INVITATION & SUPERVISOR MAGIC LINK */}
      <section id="supervisor" className="border-b bg-card">
        <div className="mx-auto max-w-[1280px] px-4 py-16 sm:px-6 lg:px-10 lg:py-24">
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
            {/* Email Invitation Flow */}
            <div className="rounded-lg border bg-canvas p-7">
              <div className="flex h-10 w-10 items-center justify-center rounded-md bg-brand-soft text-brand">
                <Mail className="h-5 w-5" strokeWidth={1.75} />
              </div>
              <h2 className="mt-4 text-xl font-bold tracking-tight text-foreground">
                Mời thành viên nhóm qua Email bảo mật
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                Owner dự án nhập địa chỉ email thành viên (hỗ trợ mọi nhà cung
                cấp email). Hệ thống gửi liên kết xác thực qua MailSender; người
                nhận đăng ký hoặc đăng nhập đúng email đó để vào thẳng dự án.
              </p>
              <ul className="mt-5 space-y-2.5 text-sm text-foreground">
                <li className="flex items-center gap-2.5">
                  <CheckCircle2
                    className="h-4 w-4 shrink-0 text-brand"
                    strokeWidth={1.75}
                  />
                  <span>Khóa kiểm tra khớp email chống truy cập trái phép</span>
                </li>
                <li className="flex items-center gap-2.5">
                  <CheckCircle2
                    className="h-4 w-4 shrink-0 text-brand"
                    strokeWidth={1.75}
                  />
                  <span>Theo dõi trạng thái Pending, gửi lại hoặc thu hồi lời mời</span>
                </li>
              </ul>
            </div>

            {/* Supervisor Zero-Auth Portal */}
            <div className="rounded-lg border bg-canvas p-7">
              <div className="flex h-10 w-10 items-center justify-center rounded-md bg-brand-soft text-brand">
                <ShieldCheck className="h-5 w-5" strokeWidth={1.75} />
              </div>
              <h2 className="mt-4 text-xl font-bold tracking-tight text-foreground">
                Cổng Giảng viên chỉ đọc qua Magic Link
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                GVHD không cần tạo tài khoản. Truy cập link riêng của nhóm để
                xem tiến độ 10 cột mốc, biểu đồ đóng góp từng sinh viên, kiểm
                tra Peer-Review và trả lời trực tiếp vào sổ Q&amp;A.
              </p>
              <ul className="mt-5 space-y-2.5 text-sm text-foreground">
                <li className="flex items-center gap-2.5">
                  <Link2
                    className="h-4 w-4 shrink-0 text-brand"
                    strokeWidth={1.75}
                  />
                  <span>Leader chủ động tạo mới hoặc thu hồi Magic Link bất cứ lúc nào</span>
                </li>
                <li className="flex items-center gap-2.5">
                  <FileSpreadsheet
                    className="h-4 w-4 shrink-0 text-brand"
                    strokeWidth={1.75}
                  />
                  <span>Tải ngay Excel Tracking 5 sheets &amp; Báo cáo tuần chuẩn FPTU</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* CLOSING & FOOTER */}
      <footer className="bg-canvas">
        <div className="mx-auto max-w-[1280px] px-4 py-14 sm:px-6 lg:px-10">
          <div className="flex flex-col items-start justify-between gap-6 border-b pb-10 md:flex-row md:items-center">
            <div>
              <h2 className="text-2xl font-bold tracking-tight text-foreground">
                Sẵn sàng khởi tạo không gian đồ án SEP490?
              </h2>
              <p className="mt-1.5 text-sm text-muted-foreground">
                Tạo dự án mới để tự động khởi sinh 10 cột mốc OGA và 5 mục tiêu
                chất lượng.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <Button asChild className="h-11 px-5">
                <Link href="/projects">
                  Tạo dự án SEP490
                  <ArrowRight className="h-4 w-4" strokeWidth={1.75} />
                </Link>
              </Button>
              <Button variant="outline" asChild className="h-11 px-5">
                <Link href="/register">Đăng ký tài khoản</Link>
              </Button>
            </div>
          </div>

          <div className="mt-8 flex flex-col items-start justify-between gap-4 text-xs text-muted-foreground sm:flex-row sm:items-center">
            <p className="font-mono">
              SEP490 Capstone Copilot • TeaMa Workspace
            </p>
            <div className="flex flex-wrap items-center gap-5">
              <Link href="/login" className="hover:text-foreground">
                Đăng nhập
              </Link>
              <Link href="/register" className="hover:text-foreground">
                Đăng ký
              </Link>
              <Link href="/projects" className="hover:text-foreground">
                Workspace
              </Link>
              <Link
                href="/supervisor/demo-token"
                className="hover:text-foreground"
              >
                Demo Giảng viên
              </Link>
            </div>
          </div>
        </div>
      </footer>
    </main>
  );
}
