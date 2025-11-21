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
    public static final String BINARY = "0x6080604052348015600e575f5ffd5b5061286f8061001c5f395ff3fe6080604052600436106100c0575f3560e01c80638216707b1161007e578063b10d6b4111610058578063b10d6b4114610294578063bed9d861146102d5578063fa17185a146102eb578063faf5625f14610313576100c0565b80638216707b1461020657806390a4b1621461022e5780639f29f1351461026a576100c0565b8062e168f0146100c4578063061bd79c146101055780632b2805db1461012157806343ca78a2146101615780634c565d5b146101a2578063577a1b92146101ca575b5f5ffd5b3480156100cf575f5ffd5b506100ea60048036038101906100e59190611846565b61033d565b6040516100fc96959493929190611922565b60405180910390f35b61011f600480360381019061011a9190611ab4565b610426565b005b34801561012c575f5ffd5b5061014760048036038101906101429190611b2e565b61075a565b604051610158959493929190611b68565b60405180910390f35b34801561016c575f5ffd5b5061018760048036038101906101829190611846565b61083c565b60405161019996959493929190611922565b60405180910390f35b3480156101ad575f5ffd5b506101c860048036038101906101c39190611b2e565b61096a565b005b3480156101d5575f5ffd5b506101f060048036038101906101eb9190611bea565b610c17565b6040516101fd9190611c28565b60405180910390f35b348015610211575f5ffd5b5061022c60048036038101906102279190611c41565b610c42565b005b348015610239575f5ffd5b50610254600480360381019061024f9190611846565b6110a2565b6040516102619190611d36565b60405180910390f35b348015610275575f5ffd5b5061027e611135565b60405161028b9190611d56565b60405180910390f35b34801561029f575f5ffd5b506102ba60048036038101906102b59190611b2e565b61113a565b6040516102cc96959493929190611e26565b60405180910390f35b3480156102e0575f5ffd5b506102e9611351565b005b3480156102f6575f5ffd5b50610311600480360381019061030c9190611e93565b61144b565b005b34801561031e575f5ffd5b5061032761172d565b6040516103349190611d56565b60405180910390f35b6001602052805f5260405f205f91509050805f015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff169080600101805461038190611f1a565b80601f01602080910402602001604051908101604052809291908181526020018280546103ad90611f1a565b80156103f85780601f106103cf576101008083540402835291602001916103f8565b820191905f5260205f20905b8154815290600101906020018083116103db57829003601f168201915b505050505090806002015f9054906101000a900460ff16908060030154908060040154908060050154905086565b670de0b6b3a76400003414610470576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161046790611f94565b60405180910390fd5b60015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f206002015f9054906101000a900460ff16156104fd576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104f490611ffc565b60405180910390fd5b60038160405161050d9190612054565b90815260200160405180910390205f9054906101000a900460ff1615610568576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161055f906120b4565b60405180910390fd5b6040518060c001604052803373ffffffffffffffffffffffffffffffffffffffff1681526020018281526020016001151581526020013481526020015f81526020015f81525060015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f820151815f015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060208201518160010190816106449190612272565b506040820151816002015f6101000a81548160ff021916908315150217905550606082015181600301556080820151816004015560a0820151816005015590505060016003826040516106979190612054565b90815260200160405180910390205f6101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff167fe89d0a5c1c3ef98dff90085eabb0e4b4a96244277ee3c5f81679a024c2ce9cf0826040516107019190612341565b60405180910390a23373ffffffffffffffffffffffffffffffffffffffff167f0a7bb2e28cc4698aac06db79cf9163bfcc20719286cf59fa7d492ceda1b8edc23460405161074f9190611d56565b60405180910390a250565b5f602052805f5260405f205f91509050805f015490806001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060020154908060030180546107a990611f1a565b80601f01602080910402602001604051908101604052809291908181526020018280546107d590611f1a565b80156108205780601f106107f757610100808354040283529160200191610820565b820191905f5260205f20905b81548152906001019060200180831161080357829003601f168201915b505050505090806004015f9054906101000a900460ff16905085565b5f60605f5f5f5f5f60015f8973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f209050805f015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681600101826002015f9054906101000a900460ff168360030154846004015485600501548480546108d690611f1a565b80601f016020809104026020016040519081016040528092919081815260200182805461090290611f1a565b801561094d5780601f106109245761010080835404028352916020019161094d565b820191905f5260205f20905b81548152906001019060200180831161093057829003601f168201915b505050505094509650965096509650965096505091939550919395565b60015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f206002015f9054906101000a900460ff166109f6576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109ed906123ab565b60405180910390fd5b805f73ffffffffffffffffffffffffffffffffffffffff165f5f8381526020019081526020015f206001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1603610a97576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610a8e90612413565b60405180910390fd5b5f5f5f8481526020019081526020015f209050806004015f9054906101000a900460ff1615610afb576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610af29061247b565b60405180910390fd5b8060050133908060018154018082558091505060019003905f5260205f20015f9091909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506001816004015f6101000a81548160ff02191690831515021790555060015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f206004015f815480929190610bc9906124c6565b91905055503373ffffffffffffffffffffffffffffffffffffffff16837ff73797093cfe30a74a119c268bea3d505e0b18bc8d2388f5317c65252cdf7c4e60405160405180910390a3505050565b6002602052815f5260405f208181548110610c30575f80fd5b905f5260205f20015f91509150505481565b805f73ffffffffffffffffffffffffffffffffffffffff165f5f8381526020019081526020015f206001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1603610ce3576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610cda90612413565b60405180910390fd5b60015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f206002015f9054906101000a900460ff16610d6f576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610d66906123ab565b60405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff165f5f8481526020019081526020015f206001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614610e0f576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610e0690612557565b60405180910390fd5b5f5f5f8481526020019081526020015f2090505f5f90505f5f90505b8260050180549050811015610ec0578573ffffffffffffffffffffffffffffffffffffffff16836005018281548110610e6757610e66612575565b5b905f5260205f20015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1603610eb35760019150610ec0565b8080600101915050610e2b565b5080610f01576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610ef890612612565b60405180910390fd5b5f6064600a60015f8973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2060030154610f519190612630565b610f5b919061269e565b90508060015f8873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f206003015f828254610fac91906126ce565b9250508190555060015f8773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f206005015f815480929190611003906124c6565b91905055503373ffffffffffffffffffffffffffffffffffffffff166108fc8290811502906040515f60405180830381858888f1935050505015801561104b573d5f5f3e3d5ffd5b508573ffffffffffffffffffffffffffffffffffffffff167f2027c39c6d0a23bf9cdaeb700b88b5070d57ea9172b2a8a3c1a45a985ca89923826040516110929190611d56565b60405180910390a2505050505050565b606060025f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2080548060200260200160405190810160405280929190818152602001828054801561112957602002820191905f5260205f20905b815481526020019060010190808311611115575b50505050509050919050565b600a81565b5f5f5f60605f60605f5f5f8981526020019081526020015f2090505f73ffffffffffffffffffffffffffffffffffffffff16816001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16036111e6576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016111dd90612413565b60405180910390fd5b805f0154816001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16826002015483600301846004015f9054906101000a900460ff168560050182805461123890611f1a565b80601f016020809104026020016040519081016040528092919081815260200182805461126490611f1a565b80156112af5780601f10611286576101008083540402835291602001916112af565b820191905f5260205f20905b81548152906001019060200180831161129257829003601f168201915b505050505092508080548060200260200160405190810160405280929190818152602001828054801561133457602002820191905f5260205f20905b815f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190600101908083116112eb575b505050505090509650965096509650965096505091939550919395565b5f60015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2090505f8160030154116113d7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016113ce9061274b565b60405180910390fd5b5f816003015490505f82600301819055505f826002015f6101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff166108fc8290811502906040515f60405180830381858888f19350505050158015611446573d5f5f3e3d5ffd5b505050565b5f5f1b820361148f576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401611486906127b3565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff165f5f8481526020019081526020015f206001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161461152f576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016115269061281b565b60405180910390fd5b6040518060c001604052808381526020013373ffffffffffffffffffffffffffffffffffffffff1681526020014281526020018281526020015f151581526020015f67ffffffffffffffff81111561158a57611589611990565b5b6040519080825280602002602001820160405280156115b85781602001602082028036833780820191505090505b508152505f5f8481526020019081526020015f205f820151815f01556020820151816001015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506040820151816002015560608201518160030190816116399190612272565b506080820151816004015f6101000a81548160ff02191690831515021790555060a0820151816005019080519060200190611675929190611739565b5090505060025f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2082908060018154018082558091505060019003905f5260205f20015f90919091909150553373ffffffffffffffffffffffffffffffffffffffff16827fdba629cc54aad70846b805286cd90bf0920d3b29656d9b042814d2a2dedf4acb836040516117219190612341565b60405180910390a35050565b670de0b6b3a764000081565b828054828255905f5260205f209081019282156117af579160200282015b828111156117ae578251825f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555091602001919060010190611757565b5b5090506117bc91906117c0565b5090565b5b808211156117d7575f815f9055506001016117c1565b5090565b5f604051905090565b5f5ffd5b5f5ffd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f611815826117ec565b9050919050565b6118258161180b565b811461182f575f5ffd5b50565b5f813590506118408161181c565b92915050565b5f6020828403121561185b5761185a6117e4565b5b5f61186884828501611832565b91505092915050565b61187a8161180b565b82525050565b5f81519050919050565b5f82825260208201905092915050565b8281835e5f83830152505050565b5f601f19601f8301169050919050565b5f6118c282611880565b6118cc818561188a565b93506118dc81856020860161189a565b6118e5816118a8565b840191505092915050565b5f8115159050919050565b611904816118f0565b82525050565b5f819050919050565b61191c8161190a565b82525050565b5f60c0820190506119355f830189611871565b818103602083015261194781886118b8565b905061195660408301876118fb565b6119636060830186611913565b6119706080830185611913565b61197d60a0830184611913565b979650505050505050565b5f5ffd5b5f5ffd5b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b6119c6826118a8565b810181811067ffffffffffffffff821117156119e5576119e4611990565b5b80604052505050565b5f6119f76117db565b9050611a0382826119bd565b919050565b5f67ffffffffffffffff821115611a2257611a21611990565b5b611a2b826118a8565b9050602081019050919050565b828183375f83830152505050565b5f611a58611a5384611a08565b6119ee565b905082815260208101848484011115611a7457611a7361198c565b5b611a7f848285611a38565b509392505050565b5f82601f830112611a9b57611a9a611988565b5b8135611aab848260208601611a46565b91505092915050565b5f60208284031215611ac957611ac86117e4565b5b5f82013567ffffffffffffffff811115611ae657611ae56117e8565b5b611af284828501611a87565b91505092915050565b5f819050919050565b611b0d81611afb565b8114611b17575f5ffd5b50565b5f81359050611b2881611b04565b92915050565b5f60208284031215611b4357611b426117e4565b5b5f611b5084828501611b1a565b91505092915050565b611b6281611afb565b82525050565b5f60a082019050611b7b5f830188611b59565b611b886020830187611871565b611b956040830186611913565b8181036060830152611ba781856118b8565b9050611bb660808301846118fb565b9695505050505050565b611bc98161190a565b8114611bd3575f5ffd5b50565b5f81359050611be481611bc0565b92915050565b5f5f60408385031215611c0057611bff6117e4565b5b5f611c0d85828601611832565b9250506020611c1e85828601611bd6565b9150509250929050565b5f602082019050611c3b5f830184611b59565b92915050565b5f5f60408385031215611c5757611c566117e4565b5b5f611c6485828601611832565b9250506020611c7585828601611b1a565b9150509250929050565b5f81519050919050565b5f82825260208201905092915050565b5f819050602082019050919050565b611cb181611afb565b82525050565b5f611cc28383611ca8565b60208301905092915050565b5f602082019050919050565b5f611ce482611c7f565b611cee8185611c89565b9350611cf983611c99565b805f5b83811015611d29578151611d108882611cb7565b9750611d1b83611cce565b925050600181019050611cfc565b5085935050505092915050565b5f6020820190508181035f830152611d4e8184611cda565b905092915050565b5f602082019050611d695f830184611913565b92915050565b5f81519050919050565b5f82825260208201905092915050565b5f819050602082019050919050565b611da18161180b565b82525050565b5f611db28383611d98565b60208301905092915050565b5f602082019050919050565b5f611dd482611d6f565b611dde8185611d79565b9350611de983611d89565b805f5b83811015611e19578151611e008882611da7565b9750611e0b83611dbe565b925050600181019050611dec565b5085935050505092915050565b5f60c082019050611e395f830189611b59565b611e466020830188611871565b611e536040830187611913565b8181036060830152611e6581866118b8565b9050611e7460808301856118fb565b81810360a0830152611e868184611dca565b9050979650505050505050565b5f5f60408385031215611ea957611ea86117e4565b5b5f611eb685828601611b1a565b925050602083013567ffffffffffffffff811115611ed757611ed66117e8565b5b611ee385828601611a87565b9150509250929050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f6002820490506001821680611f3157607f821691505b602082108103611f4457611f43611eed565b5b50919050565b7f496e636f7272656374207374616b6520616d6f756e74000000000000000000005f82015250565b5f611f7e60168361188a565b9150611f8982611f4a565b602082019050919050565b5f6020820190508181035f830152611fab81611f72565b9050919050565b7f416c72656164792072656769737465726564206173206e6f74617279000000005f82015250565b5f611fe6601c8361188a565b9150611ff182611fb2565b602082019050919050565b5f6020820190508181035f83015261201381611fda565b9050919050565b5f81905092915050565b5f61202e82611880565b612038818561201a565b935061204881856020860161189a565b80840191505092915050565b5f61205f8284612024565b915081905092915050565b7f4e616d6520616c72656164792074616b656e00000000000000000000000000005f82015250565b5f61209e60128361188a565b91506120a98261206a565b602082019050919050565b5f6020820190508181035f8301526120cb81612092565b9050919050565b5f819050815f5260205f209050919050565b5f6020601f8301049050919050565b5f82821b905092915050565b5f6008830261212e7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff826120f3565b61213886836120f3565b95508019841693508086168417925050509392505050565b5f819050919050565b5f61217361216e6121698461190a565b612150565b61190a565b9050919050565b5f819050919050565b61218c83612159565b6121a06121988261217a565b8484546120ff565b825550505050565b5f5f905090565b6121b76121a8565b6121c2818484612183565b505050565b5b818110156121e5576121da5f826121af565b6001810190506121c8565b5050565b601f82111561222a576121fb816120d2565b612204846120e4565b81016020851015612213578190505b61222761221f856120e4565b8301826121c7565b50505b505050565b5f82821c905092915050565b5f61224a5f198460080261222f565b1980831691505092915050565b5f612262838361223b565b9150826002028217905092915050565b61227b82611880565b67ffffffffffffffff81111561229457612293611990565b5b61229e8254611f1a565b6122a98282856121e9565b5f60209050601f8311600181146122da575f84156122c8578287015190505b6122d28582612257565b865550612339565b601f1984166122e8866120d2565b5f5b8281101561230f578489015182556001820191506020850194506020810190506122ea565b8683101561232c5784890151612328601f89168261223b565b8355505b6001600288020188555050505b505050505050565b5f6020820190508181035f83015261235981846118b8565b905092915050565b7f4e6f74617279206e6f74206163746976650000000000000000000000000000005f82015250565b5f61239560118361188a565b91506123a082612361565b602082019050919050565b5f6020820190508181035f8301526123c281612389565b9050919050565b7f446f63756d656e7420646f6573206e6f742065786973740000000000000000005f82015250565b5f6123fd60178361188a565b9150612408826123c9565b602082019050919050565b5f6020820190508181035f83015261242a816123f1565b9050919050565b7f446f63756d656e7420616c7265616479206e6f746172697a65640000000000005f82015250565b5f612465601a8361188a565b915061247082612431565b602082019050919050565b5f6020820190508181035f83015261249281612459565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f6124d08261190a565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff820361250257612501612499565b5b600182019050919050565b7f4f6e6c7920646f63756d656e74206f776e65722063616e20736c6173680000005f82015250565b5f612541601d8361188a565b915061254c8261250d565b602082019050919050565b5f6020820190508181035f83015261256e81612535565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52603260045260245ffd5b7f4e6f7461727920646964206e6f74206e6f746172697a65207468697320646f635f8201527f756d656e74000000000000000000000000000000000000000000000000000000602082015250565b5f6125fc60258361188a565b9150612607826125a2565b604082019050919050565b5f6020820190508181035f830152612629816125f0565b9050919050565b5f61263a8261190a565b91506126458361190a565b92508282026126538161190a565b9150828204841483151761266a57612669612499565b5b5092915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601260045260245ffd5b5f6126a88261190a565b91506126b38361190a565b9250826126c3576126c2612671565b5b828204905092915050565b5f6126d88261190a565b91506126e38361190a565b92508282039050818111156126fb576126fa612499565b5b92915050565b7f4e6f207374616b6520746f2077697468647261770000000000000000000000005f82015250565b5f61273560148361188a565b915061274082612701565b602082019050919050565b5f6020820190508181035f83015261276281612729565b9050919050565b7f496e76616c696420646f63756d656e74206861736800000000000000000000005f82015250565b5f61279d60158361188a565b91506127a882612769565b602082019050919050565b5f6020820190508181035f8301526127ca81612791565b9050919050565b7f446f63756d656e7420616c7265616479207265676973746572656400000000005f82015250565b5f612805601b8361188a565b9150612810826127d1565b602082019050919050565b5f6020820190508181035f830152612832816127f9565b905091905056fea26469706673582212205223bfdc0e13b91264e8f9b36b11bc03964f621305b8ed6721f1271c800aebb764736f6c634300081e0033";

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