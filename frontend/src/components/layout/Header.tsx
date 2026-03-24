interface HeaderProps {
  title?: string;
}

export default function Header({ title = 'Dashboard' }: HeaderProps) {
  return (
    <header className="h-14 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center px-6 shrink-0">
      <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100">{title}</h2>
    </header>
  );
}
