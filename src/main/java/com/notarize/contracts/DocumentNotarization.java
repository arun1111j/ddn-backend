package com.notarize.contracts;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * DocumentNotarization Contract Wrapper - FULLY FIXED
 *
 * Key fixes:
 * 1. deploy() method returns RemoteCall<DocumentNotarization> (NOT RemoteFunctionCall)
 * 2. All function calls return RemoteFunctionCall which requires .send()
 * 3. Proper handling of string-based IPFS CIDs
 * 4. isValid() throws IOException - must be handled
 *
 * IMPORTANT DISTINCTION:
 * - deploy() returns RemoteCall<T> - for contract deployment
 * - All other methods return RemoteFunctionCall<T> - for function calls
 */
@SuppressWarnings("rawtypes")
public class DocumentNotarization extends Contract {

    // REPLACE WITH YOUR ACTUAL COMPILED BYTECODE FROM SOLIDITY COMPILER
    public static final String BINARY = "0x6080604052348015600f57600080fd5b50611a548061001f6000396000f3fe6080604052600436106100c15760003560e01c80637ccb6a641161007f5780639f29f135116100595780639f29f13514610222578063b3dd697b14610245578063bed9d86114610276578063faf5625f1461028b57600080fd5b80637ccb6a64146101a35780638a762a84146101d557806390a4b162146101f557600080fd5b8062e168f0146100c6578063061bd79c146101015780631c49ea911461011657806343ca78a214610136578063577a1b92146101565780636a33bf8714610183575b600080fd5b3480156100d257600080fd5b506100e66100e1366004611428565b6102a7565b6040516100f89695949392919061149a565b60405180910390f35b61011461010f366004611588565b610374565b005b34801561012257600080fd5b506101146101313660046115c5565b6105e3565b34801561014257600080fd5b506100e6610151366004611428565b610810565b34801561016257600080fd5b5061017661017136600461162e565b610903565b6040516100f89190611658565b34801561018f57600080fd5b5061011461019e366004611588565b6109bc565b3480156101af57600080fd5b506101c36101be366004611588565b610b7b565b6040516100f89695949392919061166b565b3480156101e157600080fd5b506101146101f0366004611700565b610d8a565b34801561020157600080fd5b50610215610210366004611428565b6110a7565b6040516100f89190611738565b34801561022e57600080fd5b50610237600a81565b6040519081526020016100f8565b34801561025157600080fd5b50610265610260366004611588565b611196565b6040516100f895949392919061179d565b34801561028257600080fd5b506101146112f0565b34801561029757600080fd5b50610237670de0b6b3a764000081565b6001602081905260009182526040909120805491810180546001600160a01b03909316926102d4906117ed565b80601f0160208091040260200160405190810160405280929190818152602001828054610300906117ed565b801561034d5780601f106103225761010080835404028352916020019161034d565b820191906000526020600020905b81548152906001019060200180831161033057829003601f168201915b50505050600283015460038401546004850154600590950154939460ff9092169390925086565b670de0b6b3a764000034146103c95760405162461bcd60e51b8152602060048201526016602482015275125b98dbdc9c9958dd081cdd185ad948185b5bdd5b9d60521b60448201526064015b60405180910390fd5b3360009081526001602052604090206002015460ff161561042c5760405162461bcd60e51b815260206004820152601c60248201527f416c72656164792072656769737465726564206173206e6f746172790000000060448201526064016103c0565b60038160405161043c9190611827565b9081526040519081900360200190205460ff16156104915760405162461bcd60e51b81526020600482015260126024820152712730b6b29030b63932b0b23c903a30b5b2b760711b60448201526064016103c0565b6040805160c081018252338082526020808301858152600184860181905234606086015260006080860181905260a086018190529384529182905293909120825181546001600160a01b0319166001600160a01b039091161781559251919291908201906104ff9082611891565b5060408201518160020160006101000a81548160ff021916908315150217905550606082015181600301556080820151816004015560a0820151816005015590505060016003826040516105539190611827565b908152604051908190036020018120805492151560ff199093169290921790915533907fe89d0a5c1c3ef98dff90085eabb0e4b4a96244277ee3c5f81679a024c2ce9cf0906105a3908490611658565b60405180910390a260405134815233907f0a7bb2e28cc4698aac06db79cf9163bfcc20719286cf59fa7d492ceda1b8edc29060200160405180910390a250565b60008251116106275760405162461bcd60e51b815260206004820152601060248201526f125b9d985b1a590812541194c810d25160821b60448201526064016103c0565b60006001600160a01b03166000836040516106429190611827565b908152604051908190036020019020600101546001600160a01b0316146106ab5760405162461bcd60e51b815260206004820152601b60248201527f446f63756d656e7420616c72656164792072656769737465726564000000000060448201526064016103c0565b6040805160c081018252838152336020808301919091524282840152606082018490526000608083018190528351818152918201845260a083019190915291519091906106f9908590611827565b908152604051908190036020019020815181906107169082611891565b5060208201516001820180546001600160a01b0319166001600160a01b03909216919091179055604082015160028201556060820151600382019061075b9082611891565b50608082015160048201805460ff191691151591909117905560a0820151805161078f916005840191602090910190611392565b50503360009081526002602090815260408220805460018101825590835291200190506107bc8382611891565b5060405133906107cd908490611827565b60405180910390207f4d3e8366d54db979c2df7034fffffc94bdfea67b054f98b2f148d555fbe1e0df836040516108049190611658565b60405180910390a35050565b6001600160a01b038082166000908152600160208190526040822080546002820154600383015460048401546005850154958501805497986060988a98899889988998919794169560ff9091169392909190859061086d906117ed565b80601f0160208091040260200160405190810160405280929190818152602001828054610899906117ed565b80156108e65780601f106108bb576101008083540402835291602001916108e6565b820191906000526020600020905b8154815290600101906020018083116108c957829003601f168201915b505050505094509650965096509650965096505091939550919395565b6002602052816000526040600020818154811061091f57600080fd5b9060005260206000200160009150915050805461093b906117ed565b80601f0160208091040260200160405190810160405280929190818152602001828054610967906117ed565b80156109b45780601f10610989576101008083540402835291602001916109b4565b820191906000526020600020905b81548152906001019060200180831161099757829003601f168201915b505050505081565b3360009081526001602052604090206002015460ff16610a125760405162461bcd60e51b81526020600482015260116024820152704e6f74617279206e6f742061637469766560781b60448201526064016103c0565b8060006001600160a01b0316600082604051610a2e9190611827565b908152604051908190036020019020600101546001600160a01b031603610a675760405162461bcd60e51b81526004016103c090611950565b60008083604051610a789190611827565b908152604051908190036020019020600481015490915060ff1615610adf5760405162461bcd60e51b815260206004820152601a60248201527f446f63756d656e7420616c7265616479206e6f746172697a656400000000000060448201526064016103c0565b600581018054600181810183556000928352602080842090920180546001600160a01b031916339081179091556004808601805460ff19168417905590845291526040822001805491610b318361199d565b90915550506040513390610b46908590611827565b604051908190038120907f6217f0b68fdf6ca6cabe0b3236bc331e284ec5980abf567f9a2666db226b41f590600090a3505050565b606060008060606000606060008088604051610b979190611827565b90815260405190819003602001902060018101549091506001600160a01b0316610bd35760405162461bcd60e51b81526004016103c090611950565b600181015460028201546004830154835484936001600160a01b03169291600385019160ff9091169060058601908690610c0c906117ed565b80601f0160208091040260200160405190810160405280929190818152602001828054610c38906117ed565b8015610c855780601f10610c5a57610100808354040283529160200191610c85565b820191906000526020600020905b815481529060010190602001808311610c6857829003601f168201915b50505050509550828054610c98906117ed565b80601f0160208091040260200160405190810160405280929190818152602001828054610cc4906117ed565b8015610d115780601f10610ce657610100808354040283529160200191610d11565b820191906000526020600020905b815481529060010190602001808311610cf457829003601f168201915b5050505050925080805480602002602001604051908101604052809291908181526020018280548015610d6d57602002820191906000526020600020905b81546001600160a01b03168152600190910190602001808311610d4f575b505050505090509650965096509650965096505091939550919395565b8060006001600160a01b0316600082604051610da69190611827565b908152604051908190036020019020600101546001600160a01b031603610ddf5760405162461bcd60e51b81526004016103c090611950565b6001600160a01b03831660009081526001602052604090206002015460ff16610e3e5760405162461bcd60e51b81526020600482015260116024820152704e6f74617279206e6f742061637469766560781b60448201526064016103c0565b336001600160a01b0316600083604051610e589190611827565b908152604051908190036020019020600101546001600160a01b031614610ec15760405162461bcd60e51b815260206004820152601d60248201527f4f6e6c7920646f63756d656e74206f776e65722063616e20736c61736800000060448201526064016103c0565b60008083604051610ed29190611827565b908152602001604051809103902090506000805b6005830154811015610f3c57856001600160a01b0316836005018281548110610f1157610f116119b6565b6000918252602090912001546001600160a01b031603610f345760019150610f3c565b600101610ee6565b5080610f985760405162461bcd60e51b815260206004820152602560248201527f4e6f7461727920646964206e6f74206e6f746172697a65207468697320646f636044820152641d5b595b9d60da1b60648201526084016103c0565b6001600160a01b038516600090815260016020526040812060030154606490610fc390600a906119cc565b610fcd91906119e9565b6001600160a01b038716600090815260016020526040812060030180549293508392909190610ffd908490611a0b565b90915550506001600160a01b03861660009081526001602052604081206005018054916110298361199d565b9091555050604051339082156108fc029083906000818181858888f1935050505015801561105b573d6000803e3d6000fd5b50856001600160a01b03167f2027c39c6d0a23bf9cdaeb700b88b5070d57ea9172b2a8a3c1a45a985ca899238260405161109791815260200190565b60405180910390a2505050505050565b6001600160a01b0381166000908152600260209081526040808320805482518185028101850190935280835260609492939192909184015b8282101561118b5783829060005260206000200180546110fe906117ed565b80601f016020809104026020016040519081016040528092919081815260200182805461112a906117ed565b80156111775780601f1061114c57610100808354040283529160200191611177565b820191906000526020600020905b81548152906001019060200180831161115a57829003601f168201915b5050505050815260200190600101906110df565b505050509050919050565b80516020818301810180516000825292820191909301209152805481906111bc906117ed565b80601f01602080910402602001604051908101604052809291908181526020018280546111e8906117ed565b80156112355780601f1061120a57610100808354040283529160200191611235565b820191906000526020600020905b81548152906001019060200180831161121857829003601f168201915b505050506001830154600284015460038501805494956001600160a01b039093169491935090611264906117ed565b80601f0160208091040260200160405190810160405280929190818152602001828054611290906117ed565b80156112dd5780601f106112b2576101008083540402835291602001916112dd565b820191906000526020600020905b8154815290600101906020018083116112c057829003601f168201915b5050506004909301549192505060ff1685565b33600090815260016020526040902060038101546113475760405162461bcd60e51b81526020600482015260146024820152734e6f207374616b6520746f20776974686472617760601b60448201526064016103c0565b60038101805460009182905560028301805460ff191690556040519091339183156108fc0291849190818181858888f1935050505015801561138d573d6000803e3d6000fd5b505050565b8280548282559060005260206000209081019282156113e7579160200282015b828111156113e757825182546001600160a01b0319166001600160a01b039091161782556020909201916001909101906113b2565b506113f39291506113f7565b5090565b5b808211156113f357600081556001016113f8565b80356001600160a01b038116811461142357600080fd5b919050565b60006020828403121561143a57600080fd5b6114438261140c565b9392505050565b60005b8381101561146557818101518382015260200161144d565b50506000910152565b6000815180845261148681602086016020860161144a565b601f01601f19169290920160200192915050565b6001600160a01b038716815260c0602082018190526000906114be9083018861146e565b9515156040830152506060810193909352608083019190915260a09091015292915050565b634e487b7160e01b600052604160045260246000fd5b600082601f83011261150a57600080fd5b813567ffffffffffffffff811115611524576115246114e3565b604051601f8201601f19908116603f0116810167ffffffffffffffff81118282101715611553576115536114e3565b60405281815283820160200185101561156b57600080fd5b816020850160208301376000918101602001919091529392505050565b60006020828403121561159a57600080fd5b813567ffffffffffffffff8111156115b157600080fd5b6115bd848285016114f9565b949350505050565b600080604083850312156115d857600080fd5b823567ffffffffffffffff8111156115ef57600080fd5b6115fb858286016114f9565b925050602083013567ffffffffffffffff81111561161857600080fd5b611624858286016114f9565b9150509250929050565b6000806040838503121561164157600080fd5b61164a8361140c565b946020939093013593505050565b602081526000611443602083018461146e565b60c08152600061167e60c083018961146e565b6001600160a01b03881660208401526040830187905282810360608401526116a6818761146e565b851515608085015283810360a08501528451808252602080870193509091019060005b818110156116f05783516001600160a01b03168352602093840193909201916001016116c9565b50909a9950505050505050505050565b6000806040838503121561171357600080fd5b61171c8361140c565b9150602083013567ffffffffffffffff81111561161857600080fd5b6000602082016020835280845180835260408501915060408160051b86010192506020860160005b8281101561179157603f1987860301845261177c85835161146e565b94506020938401939190910190600101611760565b50929695505050505050565b60a0815260006117b060a083018861146e565b6001600160a01b03871660208401526040830186905282810360608401526117d8818661146e565b91505082151560808301529695505050505050565b600181811c9082168061180157607f821691505b60208210810361182157634e487b7160e01b600052602260045260246000fd5b50919050565b6000825161183981846020870161144a565b9190910192915050565b601f82111561138d57806000526020600020601f840160051c8101602085101561186a5750805b601f840160051c820191505b8181101561188a5760008155600101611876565b5050505050565b815167ffffffffffffffff8111156118ab576118ab6114e3565b6118bf816118b984546117ed565b84611843565b6020601f8211600181146118f357600083156118db5750848201515b600019600385901b1c1916600184901b17845561188a565b600084815260208120601f198516915b828110156119235787850151825560209485019460019092019101611903565b50848210156119415786840151600019600387901b60f8161c191681555b50505050600190811b01905550565b60208082526017908201527f446f63756d656e7420646f6573206e6f74206578697374000000000000000000604082015260600190565b634e487b7160e01b600052601160045260246000fd5b6000600182016119af576119af611987565b5060010190565b634e487b7160e01b600052603260045260246000fd5b80820281158282048414176119e3576119e3611987565b92915050565b600082611a0657634e487b7160e01b600052601260045260246000fd5b500490565b818103818111156119e3576119e361198756fea2646970667358221220ef19b13663027d50aa554927b8ef8703a818fbc6cded117fe521e38234a4a43364736f6c634300081e0033";

    // Function names
    public static final String FUNC_STAKE_AMOUNT = "STAKE_AMOUNT";
    public static final String FUNC_SLASH_PERCENTAGE = "SLASH_PERCENTAGE";
    public static final String FUNC_GETDOCUMENT = "getDocument";
    public static final String FUNC_GETUSERDOCUMENTS = "getUserDocuments";
    public static final String FUNC_GETNOTARYINFO = "getNotaryInfo";
    public static final String FUNC_NOTARIZEDOCUMENT = "notarizeDocument";
    public static final String FUNC_REGISTERASNOTARY = "registerAsNotary";
    public static final String FUNC_REGISTERDOCUMENT = "registerDocument";
    public static final String FUNC_SLASHNOTARY = "slashNotary";
    public static final String FUNC_WITHDRAWSTAKE = "withdrawStake";

    // ========== CONSTRUCTORS ==========

    protected DocumentNotarization(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    protected DocumentNotarization(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    protected DocumentNotarization(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    protected DocumentNotarization(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    // ========== STATIC LOAD METHODS ==========

    public static DocumentNotarization load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DocumentNotarization(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DocumentNotarization load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DocumentNotarization(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    public static DocumentNotarization load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DocumentNotarization(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DocumentNotarization load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DocumentNotarization(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    // ========== DEPLOYMENT METHODS - FIXED ==========

    /**
     * Deploy contract - Returns RemoteFunctionCall, must call .send()
     */
    public static RemoteCall<DocumentNotarization> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DocumentNotarization.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<DocumentNotarization> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DocumentNotarization.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DocumentNotarization> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DocumentNotarization.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DocumentNotarization> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DocumentNotarization.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    // ========== CONTRACT CONSTANTS ==========

    public RemoteFunctionCall<BigInteger> STAKE_AMOUNT() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_STAKE_AMOUNT,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {})
        );
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> SLASH_PERCENTAGE() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SLASH_PERCENTAGE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {})
        );
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    // ========== TRANSACTION METHODS ==========

    /**
     * Register as notary - payable function
     * @param _name Notary name
     * @param weiValue Amount to stake (must equal STAKE_AMOUNT)
     */
    public RemoteFunctionCall<TransactionReceipt> registerAsNotary(String _name, BigInteger weiValue) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REGISTERASNOTARY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_name)),
                Collections.<TypeReference<?>>emptyList()
        );
        return executeRemoteCallTransaction(function, weiValue);
    }

    /**
     * Register document with IPFS CID
     */
    public RemoteFunctionCall<TransactionReceipt> registerDocument(String _ipfsCid, String _documentName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REGISTERDOCUMENT,
                Arrays.<Type>asList(
                        new org.web3j.abi.datatypes.Utf8String(_ipfsCid),
                        new org.web3j.abi.datatypes.Utf8String(_documentName)
                ),
                Collections.<TypeReference<?>>emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    /**
     * Notarize document
     */
    public RemoteFunctionCall<TransactionReceipt> notarizeDocument(String _ipfsCid) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_NOTARIZEDOCUMENT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_ipfsCid)),
                Collections.<TypeReference<?>>emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    /**
     * Slash notary for fraudulent notarization
     */
    public RemoteFunctionCall<TransactionReceipt> slashNotary(String _notaryAddress, String _ipfsCid) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SLASHNOTARY,
                Arrays.<Type>asList(
                        new org.web3j.abi.datatypes.Address(_notaryAddress),
                        new org.web3j.abi.datatypes.Utf8String(_ipfsCid)
                ),
                Collections.<TypeReference<?>>emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    /**
     * Withdraw stake (notary must be inactive)
     */
    public RemoteFunctionCall<TransactionReceipt> withdrawStake() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_WITHDRAWSTAKE,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    // ========== QUERY METHODS ==========

    /**
     * Get document by IPFS CID
     */
    public RemoteFunctionCall<Document> getDocument(String _ipfsCid) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_GETDOCUMENT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_ipfsCid)),
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Bool>() {},
                        new TypeReference<DynamicArray<Address>>() {}
                )
        );

        return new RemoteFunctionCall<Document>(function,
                new java.util.concurrent.Callable<Document>() {
                    @Override
                    public Document call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Document(
                                (String) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (String) results.get(3).getValue(),
                                (Boolean) results.get(4).getValue(),
                                convertToAddressList((DynamicArray<Address>) results.get(5))
                        );
                    }
                });
    }

    /**
     * Get all documents for a user (returns IPFS CIDs)
     */
    public RemoteFunctionCall<List<String>> getUserDocuments(String _user) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_GETUSERDOCUMENTS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_user)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Utf8String>>() {})
        );

        return new RemoteFunctionCall<List<String>>(function,
                new java.util.concurrent.Callable<List<String>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List<String> call() throws Exception {
                        List<Type> result = executeCallMultipleValueReturn(function);
                        return convertToStringList((DynamicArray<Utf8String>) result.get(0));
                    }
                });
    }

    /**
     * Get notary information
     */
    public RemoteFunctionCall<NotaryInfo> getNotaryInfo(String notaryAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_GETNOTARYINFO,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(notaryAddress)),
                Arrays.<TypeReference<?>>asList(
                        new TypeReference<Address>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Bool>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}
                )
        );

        return new RemoteFunctionCall<NotaryInfo>(function,
                new java.util.concurrent.Callable<NotaryInfo>() {
                    @Override
                    public NotaryInfo call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new NotaryInfo(
                                (String) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (Boolean) results.get(2).getValue(),
                                (BigInteger) results.get(3).getValue(),
                                (BigInteger) results.get(4).getValue(),
                                (BigInteger) results.get(5).getValue()
                        );
                    }
                });
    }

    // ========== HELPER METHODS ==========

    private List<String> convertToAddressList(DynamicArray<Address> addressArray) {
        List<String> addresses = new java.util.ArrayList<>();
        for (Address address : addressArray.getValue()) {
            addresses.add(address.getValue());
        }
        return addresses;
    }

    private List<String> convertToStringList(DynamicArray<Utf8String> stringArray) {
        List<String> strings = new java.util.ArrayList<>();
        for (Utf8String str : stringArray.getValue()) {
            strings.add(str.getValue());
        }
        return strings;
    }

    // ========== DATA CLASSES ==========

    /**
     * Document structure returned from blockchain
     */
    public static class Document {
        private String ipfsCid;
        private String owner;
        private BigInteger timestamp;
        private String documentName;
        private Boolean isNotarized;
        private List<String> notaries;

        public Document(String ipfsCid, String owner, BigInteger timestamp,
                        String documentName, Boolean isNotarized, List<String> notaries) {
            this.ipfsCid = ipfsCid;
            this.owner = owner;
            this.timestamp = timestamp;
            this.documentName = documentName;
            this.isNotarized = isNotarized;
            this.notaries = notaries;
        }

        public String getIpfsCid() { return ipfsCid; }
        public String getOwner() { return owner; }
        public BigInteger getTimestamp() { return timestamp; }
        public String getDocumentName() { return documentName; }
        public Boolean getIsNotarized() { return isNotarized; }
        public List<String> getNotaries() { return notaries; }
    }

    /**
     * Notary information structure
     */
    public static class NotaryInfo {
        private String notaryAddress;
        private String name;
        private Boolean isActive;
        private BigInteger stakeAmount;
        private BigInteger successfulNotarizations;
        private BigInteger slashedCount;

        public NotaryInfo(String notaryAddress, String name, Boolean isActive,
                          BigInteger stakeAmount, BigInteger successfulNotarizations,
                          BigInteger slashedCount) {
            this.notaryAddress = notaryAddress;
            this.name = name;
            this.isActive = isActive;
            this.stakeAmount = stakeAmount;
            this.successfulNotarizations = successfulNotarizations;
            this.slashedCount = slashedCount;
        }

        public String getNotaryAddress() { return notaryAddress; }
        public String getName() { return name; }
        public Boolean getIsActive() { return isActive; }
        public BigInteger getStakeAmount() { return stakeAmount; }
        public BigInteger getSuccessfulNotarizations() { return successfulNotarizations; }
        public BigInteger getSlashedCount() { return slashedCount; }
    }
}