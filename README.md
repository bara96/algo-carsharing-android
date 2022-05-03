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

*Note*: a working indexer instance is required to use this application.  

## Account Setup
In order to use the application a valid Algorand Account must be provided.  
You can generate a new account with [goal](https://developer.algorand.org/docs/clis/goal/goal/) CLI:  
- `./sandbox goal account new`  

Remember to [fund the account](https://developer.algorand.org/docs/sdks/go/?from_query=fund#fund-account) if you're using a public network.  
If you're using a private network you can export an already funded account in this way:  
- `./sandbox goal account list`  
- `./sandbox goal account export -a {address}`  

Save the mnemonic and paste it on the Account section of the app to set the user.  

## Smart Contracts
The Smart Contracts already compiled are stored in the [assets](app/src/main/assets/contracts) folder.  
You can find the original PyTEAL version on the [Python version](https://github.com/bara96/algo-carsharing-python/tree/master/smart_contracts).  

## Environment and dependencies
- For this project an Android emulator with `API 30` are used
