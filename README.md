bitsafe-example-wallet
======================

Example wallet application for BitSafe (a hardware Bitcoin wallet).

The BitSafe, like other hardware Bitcoin wallets, can operate in "key signing mode". In this mode,
the hardware wallet can only generate keys and sign transactions. It requires another application
to scan the Bitcoin blockchain and generate transactions. This application is a quickly-hacked-up
application that does just that.

Access to the blockchain is provided through bitcoinj (https://code.google.com/p/bitcoinj/).

Quick start guide:
- Select "Device", "Connect" to connect to BitSafe
- Select "Wallets", "Create" to create a new wallet (in the future you can use Load)
- Press "Receive" button to generate a new receiving address
- Press "Send" button to bring up send dialog

Warning: this is a hacked-up demo application, with poor quality code. It is brittle and probably
has lots of bugs.
