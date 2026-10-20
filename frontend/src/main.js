import { bindShell } from './components/navigation.js';
import { createApplication } from './application.js';
bindShell();
const application = createApplication();
await application.start();
