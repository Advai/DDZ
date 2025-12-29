import { Link } from 'react-router-dom';

/**
 * Top navigation bar component.
 * Clean, minimal design matching the MVP aesthetic.
 *
 * @example
 * <TopNav />
 */
export function TopNav() {
  // TODO: Get user state from AuthContext when implemented
  const isLoggedIn = false;
  const username = '';

  return (
    <nav className="bg-background-light border-b border-gray-800">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex items-center justify-between h-16">
          {/* Logo / Brand */}
          <Link
            to="/"
            className="flex items-center hover:opacity-80 transition-opacity"
          >
            <span className="text-xl font-bold text-white">斗地主</span>
            <span className="text-lg font-semibold text-primary ml-2">DDZ</span>
          </Link>

          {/* Center Navigation Menu */}
          <div className="flex items-center space-x-8">
            <NavLink to="/rules">Rules</NavLink>
            <NavLink to="/shop">Shop</NavLink>
          </div>

          {/* Right Side - Login/User */}
          <div className="flex items-center">
            {isLoggedIn ? (
              <div className="flex items-center space-x-3">
                <span className="text-white text-sm font-medium">{username}</span>
                <button className="text-sm text-gray-400 hover:text-white transition-colors">
                  Logout
                </button>
              </div>
            ) : (
              <Link to="/login">
                <button className="text-sm font-medium text-gray-300 hover:text-white transition-colors px-4 py-2">
                  Login
                </button>
              </Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

/**
 * Navigation link component with clean hover effect
 */
function NavLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      className="text-sm font-medium text-gray-300 hover:text-white transition-colors"
    >
      {children}
    </Link>
  );
}
