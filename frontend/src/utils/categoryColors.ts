// The 8 category tag colors, in a fixed order. A category's colorSlot (0-7, assigned once by
// the backend) picks its color, so a category keeps its color forever.
// Palette: the dataviz reference categorical palette, validated against the app's gray
// surfaces (colorblind separation passes). Some slots are under 3:1 contrast on gray, which is
// why a tag always shows its name next to the dot: color is never the only signal.
export const CATEGORY_COLORS = [
  "#2a78d6", // blue
  "#eb6834", // orange
  "#1baf7a", // aqua
  "#eda100", // yellow
  "#e87ba4", // magenta
  "#008300", // green
  "#6250d6", // violet
  "#e34948", // red
];

export function categoryColor(slot: number | null | undefined) {
  return slot == null ? "#999999" : CATEGORY_COLORS[slot % CATEGORY_COLORS.length];
}
