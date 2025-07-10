import { User } from './user.model';

describe('User', () => {
  it('should create an instance', () => {
    const user = new User(1, 'testUser', 'testuser@example.com');

    expect(user).toBeTruthy();
    expect(user.username).toBe('testUser');
    expect(user.email).toBe('testuser@example.com');
  });
});
