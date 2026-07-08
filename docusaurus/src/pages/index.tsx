import Readme from '@site/docs/README.md';
import Layout from '@theme/Layout';
import MDXContent from '@theme/MDXContent';
import type { ReactNode } from 'react';

export default function Home(): ReactNode {
  return (
    <Layout
      title="Smart Workflow"
      description="Smart Workflow brings AI directly into Axon Ivy">
      <main className="container margin-vert--lg">
        <MDXContent>
          <Readme />
        </MDXContent>
      </main>
    </Layout>
  );
}
