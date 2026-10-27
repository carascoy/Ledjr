type Props = {
  title: string;
  description: string;
};

// placeholder for pages i haven't built yet so the nav doesn't go to a blank screen
function ComingSoon({ title, description }: Props) {
  return (
    <>
      <header className="page-header">
        <h1 className="page-title">{title}</h1>
      </header>
      <section className="pill coming-soon">
        <h2 className="pill-title">Coming soon</h2>
        <p className="pill-empty">{description}</p>
      </section>
    </>
  );
}

export default ComingSoon;
