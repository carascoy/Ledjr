// the 8 tag colors, order matters. colorSlot (0-7, backend sets it once) picks the color
// so a category keeps the same color forever.
// palette is from the dataviz skill, passed the colorblind check on the gray cards.
// some are under 3:1 contrast on gray, that's why tags always show the name too,
// color is never the only clue
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
