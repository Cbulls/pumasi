/** 지급 보상 미리보기: reward = floor(cost * 0.8) (백엔드 SettlementCalc와 동일 규칙) */
export function rewardPreview(cost: number): number {
  const reward = Math.floor(cost * 0.8);
  return cost >= 1 && reward < 1 ? 1 : reward;
}
