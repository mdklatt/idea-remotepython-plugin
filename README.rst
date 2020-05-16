#########################
Remote Python IDEA Plugin
#########################

|travis|

Run a remote Python application on a server, Vagrant machine, or Docker
container. This is *not* intended to be a full-fledged remote Python
interpreter, just a convenient way to run Python remotely in IDEs other than
PyCharm. There is no code assistance, debugging, or remote deployment.


=====
Usage
=====

Run Configurations
==================

A new``Remote Python`` run configurations will be installed under the
*Run->Edit Configurations...* menu.




============
Installation
============

Use *Preferences->Plugins->Install Plugin from Disk...* to install a local
copy of the plugin zip file from ``build/distributions``.


.. _travis: https://travis-ci.org/mdklatt/idea-rpython-plugin
.. _JetBrains: https://www.jetbrains.com

.. |travis| image:: https://travis-ci.org/mdklatt/idea-rpython-plugin.png
   :alt: Travis CI build status
   :target: `travis`_
