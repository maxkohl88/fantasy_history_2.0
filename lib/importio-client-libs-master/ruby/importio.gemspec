Gem::Specification.new do |s|
  s.name        = "importio"
  s.version     = "2.0.1"
  s.date        = "2014-06-18"
  s.summary     = "Ruby client library for import.io"
  s.description = "Connect to the import.io APIs using your Ruby application"
  s.authors     = ["Import.io developers"]
  s.email       = "dev@import.io"
  s.files       = ["lib/importio.rb"]
  s.homepage    = "https://import.io/data/integrate/#ruby"
  s.add_runtime_dependency 'http-cookie', '~> 1.0'
end