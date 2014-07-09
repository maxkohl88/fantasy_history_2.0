from distutils.core import setup

setup(
    name='Importio',
    version='2.0.2',
    author='Import.io Developers',
    author_email='dev@import.io',
    packages=['importio', 'importio.test'],
    scripts=[],
    url='https://import.io/data/integrate/#python',
    description='Access import.io APIs from your Python application',
    long_description=open('README.txt').read(),
    install_requires=[],
)