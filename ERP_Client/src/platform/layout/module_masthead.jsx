import { navigation_registry } from './navigation_registry';

function ModuleMasthead({ module_key }) {
  const module = navigation_registry.find((item) => item.key === module_key) || navigation_registry[0];
  return <section className={'module_masthead module_masthead_compact module_masthead_' + module.key} style={{ '--module-accent': module.color }}>
    <h1>{module.title}</h1>
  </section>;
}

export default ModuleMasthead;
