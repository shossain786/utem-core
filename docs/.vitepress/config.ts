import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'UTEM',
  description: 'Universal Test Execution Monitor — real-time test reporting dashboard',
  base: '/utem-core/',
  ignoreDeadLinks: true,

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/utem-core/favicon.svg' }],
  ],

  themeConfig: {
    logo: '/favicon.svg',

    nav: [
      { text: 'Guide', link: '/guide/getting-started' },
      { text: 'Reporters', link: '/reporters/junit5' },
      { text: 'API', link: '/api/rest' },
      { text: 'GitHub', link: 'https://github.com/shossain786/utem-core' },
      {
        text: 'v0.9.1',
        items: [
          { text: 'Releases', link: 'https://github.com/shossain786/utem-core/releases' },
          { text: 'Docker Hub', link: 'https://hub.docker.com/r/sddmhossain/utem-core' },
        ]
      }
    ],

    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Getting Started', link: '/guide/getting-started' },
          { text: 'Installation', link: '/guide/installation' },
          { text: 'Configuration', link: '/guide/configuration' },
          { text: 'Authentication', link: '/guide/auth' },
        ]
      },
      {
        text: 'Reporters',
        items: [
          { text: 'JUnit 5', link: '/reporters/junit5' },
          { text: 'TestNG', link: '/reporters/testng' },
          { text: 'Cucumber', link: '/reporters/cucumber' },
          { text: 'Jest', link: '/reporters/jest' },
          { text: 'Cypress', link: '/reporters/cypress' },
          { text: 'Playwright', link: '/reporters/playwright' },
          { text: 'pytest', link: '/reporters/pytest' },
          { text: 'Robot Framework', link: '/reporters/robot' },
        ]
      },
      {
        text: 'Reference',
        items: [
          { text: 'REST API', link: '/api/rest' },
          { text: 'Docker', link: '/api/docker' },
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/shossain786/utem-core' }
    ],

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2026 UTEM'
    },

    search: {
      provider: 'local'
    }
  }
})
