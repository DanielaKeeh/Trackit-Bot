require 'rake/testtask'
task default: :test

Rake::TestTask.new do |t|
  t.libs << 'test'
  t.warning = false
  t.pattern = 'test/**/test_*.rb'
  t.warning = false
end

namespace :db do
  desc 'Run database migrations'
  task :migrate do
    require_relative 'config_loaders/autoconfig'
    run_migrations!
  end
end
