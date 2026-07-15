import java.util.Locale;

/**
 * 품앗이폼 크레딧 계수 A/B 실험 — 표본 크기 계산기
 *
 * 두 비율(완료율/전환율 등) 비교 실험에서 변형군당 필요한 표본 수를 계산한다.
 * 외부 라이브러리 의존 없음 (역정규분포는 Acklam 근사로 직접 구현).
 *
 * 공식 (two-proportion z-test, 양측):
 *   n = (Z_{α/2} + Z_β)^2 * [p1(1-p1) + p2(1-p2)] / (p1 - p2)^2
 *
 * 사용:
 *   javac SampleSizeCalculator.java
 *   java  SampleSizeCalculator                  (기본 시나리오)
 *   java  SampleSizeCalculator 0.60 0.05 0.05 0.80 3
 *     인자: baseline  mde  alpha  power  numArms
 *
 * 한글이 ?로 깨지면 콘솔 인코딩 문제이므로 아래처럼 실행:
 *   java -Dstdout.encoding=UTF-8 SampleSizeCalculator
 */
public class SampleSizeCalculator {

    /** 표준정규 역누적분포 함수 (inverse CDF / probit), Acklam 근사. |오차| < 1.15e-9 */
    static double invNorm(double p) {
        if (p <= 0 || p >= 1) throw new IllegalArgumentException("p must be in (0,1): " + p);

        final double[] a = {-3.969683028665376e+01, 2.209460984245205e+02,
                -2.759285104469687e+02, 1.383577518672690e+02,
                -3.066479806614716e+01, 2.506628277459239e+00};
        final double[] b = {-5.447609879822406e+01, 1.615858368580409e+02,
                -1.556989798598866e+02, 6.680131188771972e+01,
                -1.328068155288572e+01};
        final double[] c = {-7.784894002430293e-03, -3.223964580411365e-01,
                -2.400758277161838e+00, -2.549732539343734e+00,
                4.374664141464968e+00, 2.938163982698783e+00};
        final double[] d = {7.784695709041462e-03, 3.224671290700398e-01,
                2.445134137142996e+00, 3.754408661907416e+00};

        final double pLow = 0.02425, pHigh = 1 - pLow;
        double q, r;

        if (p < pLow) {                       // 좌측 꼬리
            q = Math.sqrt(-2 * Math.log(p));
            return (((((c[0]*q + c[1])*q + c[2])*q + c[3])*q + c[4])*q + c[5])
                    / ((((d[0]*q + d[1])*q + d[2])*q + d[3])*q + 1);
        } else if (p <= pHigh) {              // 중앙
            q = p - 0.5;
            r = q * q;
            return (((((a[0]*r + a[1])*r + a[2])*r + a[3])*r + a[4])*r + a[5]) * q
                    / (((((b[0]*r + b[1])*r + b[2])*r + b[3])*r + b[4])*r + 1);
        } else {                              // 우측 꼬리
            q = Math.sqrt(-2 * Math.log(1 - p));
            return -(((((c[0]*q + c[1])*q + c[2])*q + c[3])*q + c[4])*q + c[5])
                    / ((((d[0]*q + d[1])*q + d[2])*q + d[3])*q + 1);
        }
    }

    /**
     * 변형군당 표본 크기 (응답/관측 건수).
     * @param baseline 기준군 비율 p1 (예: 완료율 0.60)
     * @param mde      검출할 최소 효과크기(절대), p2 = p1 ± mde
     * @param alpha    유의수준 (양측), 예 0.05
     * @param power    검정력 1-β, 예 0.80
     * @param numArms  전체 군 수 (다중비교 본페로니 보정에 사용; 단일 비교면 2)
     */
    static long perArm(double baseline, double mde, double alpha, double power, int numArms) {
        double p1 = baseline;
        double p2 = baseline + mde;            // 개선 방향 가정 (대칭이라 부호 무관)
        if (p2 >= 1.0) p2 = baseline - mde;    // 천장 넘으면 반대 방향
        if (p2 <= 0.0) throw new IllegalArgumentException("p2 out of range");

        // 다중비교 보정: 군이 여러 개면 비교 횟수만큼 alpha 분할 (본페로니)
        int comparisons = Math.max(1, numArms - 1);
        double adjAlpha = alpha / comparisons;

        double zA = invNorm(1 - adjAlpha / 2);  // 양측
        double zB = invNorm(power);

        double num = Math.pow(zA + zB, 2) * (p1 * (1 - p1) + p2 * (1 - p2));
        double den = Math.pow(p1 - p2, 2);
        return (long) Math.ceil(num / den);
    }

    static void report(String label, double baseline, double mde,
                       double alpha, double power, int numArms) {
        long perArm = perArm(baseline, mde, alpha, power, numArms);
        long total  = perArm * numArms;
        int comparisons = Math.max(1, numArms - 1);
        double adjAlpha = alpha / comparisons;

        System.out.printf(Locale.US, "── %s ──%n", label);
        System.out.printf(Locale.US, "  baseline p1      : %.1f%%%n", baseline * 100);
        System.out.printf(Locale.US, "  MDE (절대)       : ±%.1f%%p  (p2 = %.1f%%)%n",
                mde * 100, (baseline + mde) * 100);
        System.out.printf(Locale.US, "  alpha / power    : %.3f / %.0f%%%n", alpha, power * 100);
        System.out.printf(Locale.US, "  군 수 / 비교 수  : %d개 / %d회  (보정 alpha=%.4f)%n",
                numArms, comparisons, adjAlpha);
        System.out.printf(Locale.US, "  변형군당 표본    : %,d건%n", perArm);
        System.out.printf(Locale.US, "  전체 표본        : %,d건%n%n", total);
    }

    public static void main(String[] args) {
        System.out.println("품앗이폼 — 크레딧 계수 A/B 실험 표본 크기 계산기\n");

        if (args.length >= 5) {
            double baseline = Double.parseDouble(args[0]);
            double mde      = Double.parseDouble(args[1]);
            double alpha    = Double.parseDouble(args[2]);
            double power    = Double.parseDouble(args[3]);
            int    numArms  = Integer.parseInt(args[4]);
            report("사용자 지정 시나리오", baseline, mde, alpha, power, numArms);
            return;
        }

        // 기본 시나리오 (실험설계서 4-1 가정값; 실제 baseline은 파일럿으로 교체할 것)
        report("실험 A — 주관식 가중 W_essay (완료율, 3군)",
                0.60, 0.05, 0.05, 0.80, 3);
        report("실험 B — 지급률 payout_rate (전환율, 3군)",
                0.20, 0.04, 0.05, 0.80, 3);
        report("참고 — 단일 비교(2군), MDE 3%p",
                0.60, 0.03, 0.05, 0.80, 2);

        System.out.println("주의: baseline·MDE는 예시 가정값입니다.");
        System.out.println("파일럿 1~2주로 실제 baseline을 확보한 뒤 인자로 넣어 재계산하세요.");
        System.out.println("예) java SampleSizeCalculator 0.55 0.05 0.05 0.80 3");
    }
}
