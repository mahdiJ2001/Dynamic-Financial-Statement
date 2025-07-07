import { FinancialStatement } from './financial-statement.model';
import { User } from './user.model';

describe('FinancialStatement', () => {
  it('should create an instance', () => {
    const user = new User(1, 'testUser', 'testuser@example.com');
    const financialStatement = new FinancialStatement(
      1,
      'some form data',
      'report content',
      '2025-04-28T00:00:00Z',
      user
    );

    expect(financialStatement).toBeTruthy();
    expect(financialStatement.createdBy).toBeInstanceOf(User);
  });
});
