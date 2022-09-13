:: Helper script for the SecureShellState class to pass a password value to
:: `ssh` as part of the OpenSSH SSH_ASKPASS protocol.
:: <https://linux.die.net/man/1/ssh>
:: TODO: This is untested.
@echo off
echo %SSH_PASSWORD%
set SSH_PASSWORD=
