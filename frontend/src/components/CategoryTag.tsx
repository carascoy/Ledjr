import { categoryColor } from "../utils/categoryColors";

type Props = {
  name: string;
  colorSlot: number | null | undefined;
};

// little chip: colored dot + name. text stays neutral, the dot has the color
function CategoryTag({ name, colorSlot }: Props) {
  return (
    <span className="cat-tag">
      <i className="cat-dot" style={{ background: categoryColor(colorSlot) }} />
      {name}
    </span>
  );
}

export default CategoryTag;
