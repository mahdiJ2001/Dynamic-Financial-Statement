import { Template } from './template.model';

describe('Template', () => {
  it('should create an instance', () => {
    const template = new Template(1, 'Bilan Mai', '{"fields": ["immobilisations"]}', '2025-05-10T11:00:00');
    expect(template).toBeTruthy();
  });
});
