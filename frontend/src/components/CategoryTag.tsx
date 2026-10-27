import { categoryColor } from "../utils/categoryColors";

type Props = {
  name: string;
  colorSlot: number | null | undefined;
};

// A small rounded chip: colored dot + name. The text stays neutral; the dot carries the color.
function CategoryTag({ name, colorSlot }: Props) {
  return (
    <span className="cat-tag">
      <i className="cat-dot" style={{ background: categoryColor(colorSlot) }} />
      {name}
    </span>
  );
}

export default CategoryTag;
