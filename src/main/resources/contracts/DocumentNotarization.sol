// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * DocumentNotarization Contract
 * FIXED: Uses string for IPFS CIDs instead of bytes32
 */
contract DocumentNotarization {
    // Constants
    uint256 public constant STAKE_AMOUNT = 1 ether;
    uint256 public constant SLASH_PERCENTAGE = 10; // 10%

    struct Document {
        string ipfsCid;           // CHANGED: string instead of bytes32
        address owner;
        uint256 timestamp;
        string documentName;
        bool isNotarized;
        address[] notaries;
    }

    struct NotaryInfo {
        address notaryAddress;
        string name;
        bool isActive;
        uint256 stakeAmount;
        uint256 successfulNotarizations;
        uint256 slashedCount;
    }

    // State variables - CHANGED: mapping uses string now
    mapping(string => Document) public documents;
    mapping(address => NotaryInfo) public notaries;
    mapping(address => string[]) public userDocuments; // CHANGED: string[] not bytes32[]
    mapping(string => bool) private usedNames;

    // Events - CHANGED: indexed string for IPFS CID
    event DocumentRegistered(string indexed ipfsCid, address indexed owner, string documentName);
    event DocumentNotarized(string indexed ipfsCid, address indexed notary);
    event NotaryRegistered(address indexed notary, string name);
    event NotarySlashed(address indexed notary, uint256 amount);
    event StakeDeposited(address indexed notary, uint256 amount);

    // Modifiers
    modifier onlyActiveNotary() {
        require(notaries[msg.sender].isActive, "Notary not active");
        _;
    }

    modifier documentExists(string memory _ipfsCid) {
        require(documents[_ipfsCid].owner != address(0), "Document does not exist");
        _;
    }

    // Register as notary
    function registerAsNotary(string memory _name) public payable {
        require(msg.value == STAKE_AMOUNT, "Incorrect stake amount");
        require(!notaries[msg.sender].isActive, "Already registered as notary");
        require(!usedNames[_name], "Name already taken");

        notaries[msg.sender] = NotaryInfo({
            notaryAddress: msg.sender,
            name: _name,
            isActive: true,
            stakeAmount: msg.value,
            successfulNotarizations: 0,
            slashedCount: 0
        });

        usedNames[_name] = true;
        emit NotaryRegistered(msg.sender, _name);
        emit StakeDeposited(msg.sender, msg.value);
    }

    // Register document with IPFS CID
    function registerDocument(string memory _ipfsCid, string memory _documentName) public {
        require(bytes(_ipfsCid).length > 0, "Invalid IPFS CID");
        require(documents[_ipfsCid].owner == address(0), "Document already registered");

        documents[_ipfsCid] = Document({
            ipfsCid: _ipfsCid,
            owner: msg.sender,
            timestamp: block.timestamp,
            documentName: _documentName,
            isNotarized: false,
            notaries: new address[](0)
        });

        userDocuments[msg.sender].push(_ipfsCid);
        emit DocumentRegistered(_ipfsCid, msg.sender, _documentName);
    }

    // Notarize document
    function notarizeDocument(string memory _ipfsCid)
    public
    onlyActiveNotary
    documentExists(_ipfsCid)
    {
        Document storage doc = documents[_ipfsCid];
        require(!doc.isNotarized, "Document already notarized");

        doc.notaries.push(msg.sender);
        doc.isNotarized = true;
        notaries[msg.sender].successfulNotarizations++;

        emit DocumentNotarized(_ipfsCid, msg.sender);
    }

    // Slash notary
    function slashNotary(address _notaryAddress, string memory _ipfsCid)
    public
    documentExists(_ipfsCid)
    {
        require(notaries[_notaryAddress].isActive, "Notary not active");
        require(documents[_ipfsCid].owner == msg.sender, "Only document owner can slash");

        // Check if this notary notarized the document
        Document storage doc = documents[_ipfsCid];
        bool found = false;
        for (uint i = 0; i < doc.notaries.length; i++) {
            if (doc.notaries[i] == _notaryAddress) {
                found = true;
                break;
            }
        }
        require(found, "Notary did not notarize this document");

        uint256 slashAmount = (notaries[_notaryAddress].stakeAmount * SLASH_PERCENTAGE) / 100;
        notaries[_notaryAddress].stakeAmount -= slashAmount;
        notaries[_notaryAddress].slashedCount++;

        payable(msg.sender).transfer(slashAmount);
        emit NotarySlashed(_notaryAddress, slashAmount);
    }

    // Withdraw stake
    function withdrawStake() public {
        NotaryInfo storage notary = notaries[msg.sender];
        require(notary.stakeAmount > 0, "No stake to withdraw");

        uint256 amount = notary.stakeAmount;
        notary.stakeAmount = 0;
        notary.isActive = false;

        payable(msg.sender).transfer(amount);
    }

    // Get document by IPFS CID
    function getDocument(string memory _ipfsCid) public view returns (
        string memory ipfsCid,
        address owner,
        uint256 timestamp,
        string memory documentName,
        bool isNotarized,
        address[] memory notariesList
    ) {
        Document storage doc = documents[_ipfsCid];
        require(doc.owner != address(0), "Document does not exist");

        return (
            doc.ipfsCid,
            doc.owner,
            doc.timestamp,
            doc.documentName,
            doc.isNotarized,
            doc.notaries
        );
    }

    // Get user documents (returns IPFS CIDs)
    function getUserDocuments(address _user) public view returns (string[] memory) {
        return userDocuments[_user];
    }

    // Get notary info
    function getNotaryInfo(address _notary) public view returns (
        address notaryAddress,
        string memory name,
        bool isActive,
        uint256 stakeAmount,
        uint256 successfulNotarizations,
        uint256 slashedCount
    ) {
        NotaryInfo storage notary = notaries[_notary];
        return (
            notary.notaryAddress,
            notary.name,
            notary.isActive,
            notary.stakeAmount,
            notary.successfulNotarizations,
            notary.slashedCount
        );
    }
}