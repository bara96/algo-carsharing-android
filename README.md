# Algorand Carsharing Android
Smart Contract developed in [PyTeal](https://developer.algorand.org/docs/get-details/dapps/pyteal/) to build a dApp for car sharing in Algorand Blockchain.  
Application developed in Java.  
Linked to [Algorand Carsharing Python](https://github.com/bara96/algo-carsharing-python) project.  

# Requirements
- [Android studio 4.1 or higher](https://developer.android.com/studio)
- [Java SDK 1.8.0 or higher](https://www.oracle.com/java/technologies/downloads/)
- [Docker](https://www.docker.com/products/docker-desktop)
- [Docker Compose](https://docs.docker.com/compose/)

## Sandbox environment
In order to deploy any transaction on Algorand Blockchain you need first to set up an Algorand node.  
You can easily set up an Algorand **sandbox** node following the [official guide](https://github.com/algorand/sandbox#algorand-sandbox) or following [this](https://developer.algorand.org/docs/get-started/dapps/pyteal/#install-sandbox) tutorial.  
The project version is **v0.68**

To start the **Testnet** sandbox:
- `./sandbox up testnet`

On testnet the indexer is not enabled, in order to use it you can switch on release sandbox:
- `./sandbox up release`

## Environment and dependencies
- For this project an Android emulator with `API 30` are used
