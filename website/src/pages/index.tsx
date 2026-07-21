import Link from '@docusaurus/Link';
import { useVersions } from '@docusaurus/plugin-content-docs/client';
import Translate, { translate } from '@docusaurus/Translate';
import Heading from '@theme/Heading';
import Layout from '@theme/Layout';
import clsx from 'clsx';
import type { ReactNode } from 'react';
import styles from './index.module.css';

function HomepageHeader() {
  const versions = useVersions('default');
  // Find the 'current' (next) version, which is the unreleased version corresponding to the docs/ folder
  const currentVersion = versions.find((v) => v.name === 'current') || versions[0];
  
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          Smart Workflow
        </Heading>
        <p className="hero__subtitle">
          <Translate id="homepage.subtitle">Bring AI directly into Axon Ivy</Translate>
        </p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to={`${currentVersion.path}/overview`}>
            <Translate id="homepage.exploreButton">Explore Overview</Translate>
          </Link>
        </div>
      </div>
    </header>
  );
}

const FeatureList = [
  {
    title: translate({id: 'homepage.feature.setup', message: 'Familiar Setup'}),
    description: (
      <Translate id="homepage.feature.setup.desc">
        Drop AI agents into BPMN processes with no structural changes and configure everything through Axon Ivy’s standard interfaces.
      </Translate>
    ),
  },
  {
    title: translate({id: 'homepage.feature.enterprise', message: 'Enterprise-ready'}),
    description: (
      <Translate id="homepage.feature.enterprise.desc">
        Built for enterprise needs with logging, monitoring, and configuration controls natively supported out of the box.
      </Translate>
    ),
  },
  {
    title: translate({id: 'homepage.feature.tools', message: 'Flexible Tools'}),
    description: (
      <Translate id="homepage.feature.tools.desc">
        Turn any callable process into an AI-discoverable tool, empowering agents to act autonomously.
      </Translate>
    ),
  },
  {
    title: translate({id: 'homepage.feature.models', message: 'Multi-model Support'}),
    description: (
      <Translate id="homepage.feature.models.desc">
        Use lightweight or advanced language models seamlessly depending on the complexity of the task at hand.
      </Translate>
    ),
  },
  {
    title: translate({id: 'homepage.feature.outputs', message: 'Type-safe Outputs'}),
    description: (
      <Translate id="homepage.feature.outputs.desc">
        Produce structured Java objects directly from AI responses for immediate and predictable use in your workflow.
      </Translate>
    ),
  },
  {
    title: translate({id: 'homepage.feature.nlp', message: 'Natural Language Handling'}),
    description: (
      <Translate id="homepage.feature.nlp.desc">
        Accept unstructured, conversational input and return human-friendly, synthesized output.
      </Translate>
    ),
  },
];

function HomepageFeatures() {
  return (
    <section className="padding-vert--xl">
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <div key={idx} className={clsx('col col--4', 'margin-bottom--lg')}>
              <div className="text--center padding-horiz--md">
                <Heading as="h3">{props.title}</Heading>
                <p>{props.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default function Home(): ReactNode {
  return (
    <Layout
      title={translate({id: 'homepage.title', message: 'Smart Workflow'})}
      description={translate({id: 'homepage.description', message: 'Smart Workflow brings AI directly into Axon Ivy'})}>
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
    </Layout>
  );
}
